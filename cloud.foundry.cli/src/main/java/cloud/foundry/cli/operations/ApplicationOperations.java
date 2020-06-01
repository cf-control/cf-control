package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Handles the operations for manipulating applications on a cloud foundry instance.
 */
public class ApplicationOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    /**
     * Name of the environment variable that hold the docker password.
     */
    private static final String DOCKER_PASSWORD_VAR_NAME = "CF_DOCKER_PASSWORD";

    public ApplicationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * This method fetches applications data from the cloud foundry instance.
     * To retrieve data given by the Mono object you can use subscription methods (block, subscribe, etc.)
     * provided by the reactor library method.
     * For more details on how to work with Mono's visit:
     * @return Mono object of ApplicationBeans
     */
    public Mono<List<ApplicationBean>> getAll() {
        return this.cloudFoundryOperations
                .applications()
                .list()
                .flatMap(this::getApplicationManifest)
                .map(ApplicationBean::new)
                .collectList();
    }

    private Mono<ApplicationManifest> getApplicationManifest(ApplicationSummary applicationSummary) {
        return this.cloudFoundryOperations
                .applications()
                .getApplicationManifest(GetApplicationManifestRequest
                        .builder()
                        .name(applicationSummary.getName())
                        .build());
    }

    /**
     *
     *  Pushes the app to the cloud foundry instance specified within the cloud foundry operations instance
     *
     * @param bean  application bean that holds the configuration settings to deploy the app
     *              to the cloud foundry instance
     * @param shouldStart   if the app should start after being created
     * @throws NullPointerException when bean or app name is null
     * or docker password was not set in environment variables when creating app via dockerImage and docker credentials
     * @throws IllegalArgumentException when neither a path nor a docker image were specified, or app name empty
     * @throws CreationException when app already exists
     * or any fatal error occurs during creation of the app
     * @throws SecurityException when there is no permission to access environment variable CF_DOCKER_PASSWORD
     */
    public void create(ApplicationBean bean, boolean shouldStart) throws CreationException {
        checkNotNull(bean.getName());
        checkArgument(!bean.getName().isEmpty(), "empty name");
        checkNotNull(bean);

        // useful, otherwise cloud foundry operations library might behave in a weird way
        // path null + docker image null => NullPointer Exception that is not intuitive
        // and when setting docker image to empty string to prevent this
        // can lead to clash when path and buildpack was set
        checkIfPathOrDockerGiven(bean);

        // this check is important, otherwise an app could get overwritten
        if (appExists(bean.getName())) {
            throw new CreationException("app exists already");
        }

        doCreate(bean, shouldStart);
    }

    private void doCreate(ApplicationBean bean, boolean shouldStart) throws CreationException {
        try {
            pushAppManifest(bean, shouldStart);
        } catch (IllegalArgumentException e) {
            cleanUp(bean);
            throw new CreationException(e);
        }
    }

    private void cleanUp(ApplicationBean bean) {
        // Could fail when app wasn't created, but that's ok, because we just wanted to delete it anyway.
        try {
            this.cloudFoundryOperations
                    .applications()
                    .delete(DeleteApplicationRequest
                            .builder()
                            .name(bean.getName())
                            .build())
                    .block();
        } catch (Exception e ) {
            //TODO: log warn or log debug
        }
    }

    /**
     * TODO: Clarification with project owner necessary:
     * TODO: how to proceed when push fails to apply some settings?
     * TODO: remove on fail ?
     * TODO: keep on fail and only print errors as warnings ?
     *
     * for now keep on fail with error messages
     */
    private void pushAppManifest(ApplicationBean bean, boolean shouldStart) {
        this.cloudFoundryOperations
                .applications()
                .pushManifest(PushApplicationManifestRequest
                        .builder()
                        .manifest(buildApplicationManifest(bean))
                        .noStart(!shouldStart)
                        .build())
                // Cloud Foundry Operations Library Throws either IllegalArgumentException or IllegalStateException.
                .onErrorContinue(throwable -> throwable instanceof IllegalArgumentException
                                //Fatal errors, exclude them.
                                && !throwable.getMessage().contains("Application")
                                && !throwable.getMessage().contains("Stack"),
                        (throwable, o) -> Log.warn(throwable.getMessage()))
                //Error when staging or starting. So don't throw error, only log error.
                .onErrorContinue(throwable -> throwable instanceof IllegalStateException,
                        (throwable, o) -> Log.warn(throwable.getMessage()))
                .block();
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
                            .password(getDockerPassword(bean))
                            .build())
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

    private String getDockerPassword(ApplicationBean bean) {
        if (bean.getManifest().getDockerImage() == null  || bean.getManifest().getDockerUsername() == null) {
            return null;
        }

        //TODO: Maybe outsource retrieving env variables to a dedicated class in a future feature.
        String password = System.getenv(DOCKER_PASSWORD_VAR_NAME);
        if (password == null) {
            throw new NullPointerException("Docker password not set in environment variable: "
                    + DOCKER_PASSWORD_VAR_NAME);
        }
        return password;
    }

    private List<Route> getAppRoutes(List<String> routes) {
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
        // If app does not exists an IllegalArgumentException will be thrown.
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