package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
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
     * @param bean  application bean
     * @param noStart   if the app should not start after being created
     * @throws CreationException
     */
    public void create(ApplicationBean bean, boolean noStart) throws CreationException {
        checkNotNull(bean.getName());
        checkNotEmpty(bean.getName());
        checkNotNull(bean);
        //otherwise IllegalArgumentException is thrown within reactor stream which is difficult to handle
        //TODO directly handle error in stream if there is more knowledge about how to do that
        checkIfPathOrDockerGiven(bean);
        // this check is important, otherwise an app could get overwritten
        checkAppNotExists(bean.getName());

        doCreate(bean, noStart);
    }

    private void doCreate(ApplicationBean bean, boolean noStart) throws CreationException {
        try {
            pushAppManifest(bean, noStart);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CreationException("FAILED: " + e.getMessage());
        }
    }

    private void pushAppManifest(ApplicationBean bean, boolean noStart) {
        List<Throwable> errors = new LinkedList<>();

        this.cloudFoundryOperations
                .applications()
                .pushManifest(PushApplicationManifestRequest
                        .builder()
                        .manifest(buildApplicationManifest(bean))
                        .noStart(noStart)
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
    private void checkAppExists(String name) {
        // if app does not exists an IllegalArgumentException will be thrown
        this.cloudFoundryOperations
                .applications()
                .get(GetApplicationRequest
                        .builder()
                        .name(name)
                        .build())
                .block();
    }

    /**
     * assertion method
     */
    private void checkAppNotExists(String name) throws CreationException {
        // if an app does not exist it will throw an IllegalArgumentException so return without fail
        try {
            checkAppExists(name);
        } catch ( IllegalArgumentException e) {
            return;
        }

        // if an app does exist it doesn't throw an error, so throw an error
        throw new CreationException("FAILED: app exists already");
    }

    /**
     * assertion method
     */
    private void checkNotEmpty(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty string");
        }
    }

    /**
     * assertion method
     */
    private void checkIfPathOrDockerGiven(ApplicationBean bean) {
        if (bean.getPath() == null && bean.getManifest() == null) {
            throw new IllegalArgumentException("app path or docker image must be given");
        } else if (bean.getPath() == null
                && bean.getManifest() != null
                && bean.getManifest().getDockerImage() == null) {
            throw new IllegalArgumentException("app path or docker image must be given");
        }
    }

}
