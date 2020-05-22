package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Handles the operations for manipulating applications on a cloud foundry instance.
 */
public class ApplicationOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public ApplicationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    public List<ApplicationBean> getAll() {
        List<ApplicationSummary> applications = this.cloudFoundryOperations
                .applications()
                .list()
                .collectList()
                .block();

        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ApplicationBean> beans = new ArrayList<>();
        for (ApplicationSummary summary : applications) {
            ApplicationManifest manifest = getApplicationManifest(summary);

            beans.add(new ApplicationBean(manifest));
        }

        return beans;
    }

    private ApplicationManifest getApplicationManifest(ApplicationSummary applicationSummary) {
        return this.cloudFoundryOperations
                .applications()
                .getApplicationManifest(GetApplicationManifestRequest
                        .builder()
                        .name(applicationSummary.getName())
                        .build())
                .block();
    }

    /**
     * TODO: Clarification with project owner necessary:
     * TODO: how to proceed when push fails to apply some settings?
     * TODO: remove on fail ?
     * TODO: keep on fail and only print errors as warnings ?
     *
     * for now keep on fail with error messages
     */
    /**
     *
     *  Pushes the app to the cloud foundry instance specified within the cloud foundry operations instance
     *
     * @param bean  application bean that holds the configuration settings to deploy the app to the cloud foundry instance
     * @param shouldStart   if the app should not start after being created
     * @throws NullPointerException when bean or app name is null
     * @throws IllegalArgumentException when neither a path nor a docker image were specified, or app name empty
     * @throws CreationException when app already exists or any fatal error occurs during creation of the app
     */
    public void create(ApplicationBean bean, boolean shouldStart) throws CreationException {
        checkNotNull(bean.getName());
        checkNotEmpty(bean.getName());
        checkNotNull(bean);

        // useful, otherwise 3rd party library might behave in a weird way
        // path null + docker image null => NullPointer Exception that is not intuitive
        // and when setting docker image to empty string to prevent this can lead to clash when path and buildpack was set
        checkIfPathOrDockerGiven(bean);

        // this check is important, otherwise an app could get overwritten
        if(appExists(bean.getName())){
            throw new CreationException("app exists already");
        }


        doCreate(bean, shouldStart);
    }

    private void doCreate(ApplicationBean bean, boolean shouldStart) throws CreationException {
        try {
            pushAppManifest(bean, shouldStart);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CreationException("FAILED: " + e.getMessage());
        }
    }

    private void pushAppManifest(ApplicationBean bean, boolean shouldStart) {
        List<Throwable> errors = new LinkedList<>();

        this.cloudFoundryOperations
                .applications()
                .pushManifest(PushApplicationManifestRequest
                        .builder()
                        .manifest(buildApplicationManifest(bean))
                        .noStart(!shouldStart)
                        .build())
                .onErrorContinue((throwable, o) -> errors.add(throwable))
                .block();

        //TODO: temporary error printing, will be replaced at a future date
        errors.forEach(throwable -> {
            System.out.println(throwable.getMessage());
        } );
    }

    private ApplicationManifest buildApplicationManifest(ApplicationBean bean) {
        ApplicationManifest.Builder builder = ApplicationManifest.builder();

        builder
            .name(bean.getName())
            .path(bean.getPath() == null ? null : Paths.get(bean.getPath()));

        if (bean.getManifest() != null) {
            builder.buildpack(bean.getManifest().getBuildpack())
                .command(bean.getManifest().getCommand())
                .disk(bean.getManifest().getDisk())
                .docker(Docker.builder()
                        .image(bean.getManifest().getDockerImage())
                        .username(bean.getManifest().getDockerUsername())
                        .password(null /*TODO: fetch environment variable*/).build())
                .healthCheckHttpEndpoint(bean.getManifest().getHealthCheckHttpEndpoint())
                .healthCheckType(bean.getManifest().getHealthCheckType())
                .instances(bean.getManifest().getInstances())
                .memory(bean.getManifest().getMemory())
                .noRoute(bean.getManifest().getNoRoute())
                .routePath(bean.getManifest().getRoutePath())
                .randomRoute(bean.getManifest().getRandomRoute())
                .routes(getAppRoutes(bean.getManifest().getRoutes()))
                .stack(bean.getManifest().getStack())
                .timeout(bean.getManifest().getTimeout())
                .putAllEnvironmentVariables(Optional.ofNullable(bean.getManifest().getEnvironmentVariables())
                        .orElse(Collections.emptyMap()))
                .services(bean.getManifest().getServices());
        }

        return builder.build();
    }

    public List<Route> getAppRoutes(List<String> routes) {
        return routes == null ? null : routes
                .stream()
                .filter(Objects::nonNull)
                .map(route -> Route.builder().route(route).build())
                .collect(Collectors.toList());
    }

    /**
     * assertion method
     */
    private boolean appExists(String name) {
        // if app does not exists an IllegalArgumentException will be thrown
        try {
            this.cloudFoundryOperations
                    .applications()
                    .get(GetApplicationRequest
                            .builder()
                            .name(name)
                            .build())
                    .block();
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * assertion method
     */
    private void checkNotEmpty(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }
    }

    /**
     * assertion method
     */
    private void checkIfPathOrDockerGiven(ApplicationBean bean) {
        String message = "app path or docker image must be given";
        if (bean.getPath() == null && bean.getManifest() == null) {
            throw new IllegalArgumentException(message);
        } else if (bean.getPath() == null
                && bean.getManifest() != null
                && bean.getManifest().getDockerImage() == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
