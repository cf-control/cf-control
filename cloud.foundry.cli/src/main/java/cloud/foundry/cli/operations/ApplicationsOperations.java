package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Handles the operations for manipulating applications on a cloud foundry instance.
 */
public class ApplicationsOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    /**
     * Name of the environment variable that hold the docker password.
     */
    private static final String DOCKER_PASSWORD_VAR_NAME = "CF_DOCKER_PASSWORD";

    public ApplicationsOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * This method fetches applications data from the cloud foundry instance.
     * To retrieve data given by the Mono object you can use the subscription methods (block, subscribe, etc.) provided
     * by the reactor library.
     * For more details on how to work with Mono's visit:
     * https://projectreactor.io/docs/core/release/reference/index.html#core-features
     *
     * @return Mono object of all applications as list of ApplicationBeans
     */
    public Mono<Map<String, ApplicationBean>> getAll() {
        return this.cloudFoundryOperations
                .applications()
                .list()
                .flatMap(this::getApplicationManifest)
                .collectMap(ApplicationManifest::getName, ApplicationBean::new);
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
     * Deletes a specific application associated with the name <code>applicationName</code>.
     *
     * @param applicationName applicationName Name of an application.
     * @throws NullPointerException when the applicationName is null
     * @return Mono which can be subscribed on to trigger the request to the cf instance
     */
    public Mono<Void> remove(String applicationName) {
        checkNotNull(applicationName);

        DeleteApplicationRequest request = DeleteApplicationRequest
                .builder()
                .name(applicationName)
                .build();

        return this.cloudFoundryOperations.applications()
                .delete(request)
                .doOnSuccess(aVoid -> {
                    Log.info("Application " + applicationName + " has been successfully removed.");
                });
    }


    /**
     * Pushes the app to the cloud foundry instance specified within the cloud foundry operations instance
     *
     * @param appName     name of the application
     * @param bean        application bean that holds the configuration settings to deploy the app
     *                    to the cloud foundry instance
     * @param shouldStart if the app should start after being created
     * @throws NullPointerException     when bean or app name is null
     *                                  or docker password was not set in environment variables when creating app via
     *                                  dockerImage and docker credentials
     * @throws IllegalArgumentException    when neither a path nor a docker image were specified, or app name empty
     * @throws CreationException        when any fatal error occurs during creation of the app
     * @throws SecurityException        when there is no permission to access environment variable CF_DOCKER_PASSWORD
     * @return Mono which can be subscribed on to trigger the request to the cf instance
     */
    public Mono<Void> create(String appName, ApplicationBean bean, boolean shouldStart) {
        checkNotNull(appName);
        checkArgument(!appName.isEmpty(), "empty name");
        checkNotNull(bean);

        try {
            return doCreate(appName, bean, shouldStart);
        } catch (RuntimeException e) {
            throw new CreationException(e);
        }
    }

    private Mono<Void> doCreate(String appName, ApplicationBean bean, boolean shouldStart) {
        return this.cloudFoundryOperations
                .applications()
                .pushManifest(PushApplicationManifestRequest
                        .builder()
                        .manifest(buildApplicationManifest(appName, bean))
                        .noStart(!shouldStart)
                        .build())
                .onErrorContinue(this::whenServiceNotFound, (throwable, o) -> Log.warning(throwable.getMessage()))
                .doOnSubscribe(subscription -> {
                    Log.debug("Create app:", appName);
                    Log.debug("Bean of the app:", bean);
                    Log.debug("Should the app start:", shouldStart);
                })
                .doOnSuccess(aVoid -> Log.info("App created:", appName));
    }

    private boolean whenServiceNotFound(Throwable throwable) {
        return throwable instanceof IllegalArgumentException
                && throwable.getMessage().contains("Service instance")
                && throwable.getMessage().contains("could not be found");
    }

    private ApplicationManifest buildApplicationManifest(String appName, ApplicationBean bean) {
        if (bean.getManifest() == null) {
            bean.setManifest(new ApplicationManifestBean());
        }

        return ApplicationManifest.builder()
                .name(appName)
                .path(bean.getPath() == null ? null : Paths.get(bean.getPath()))
                .buildpack(bean.getManifest().getBuildpack())
                .command(bean.getManifest().getCommand())
                .disk(bean.getManifest().getDisk())
                .docker(Docker.builder()
                        .image(bean.getPath() == null && bean.getManifest().getDockerImage() == null ? "" :bean.getManifest().getDockerImage())
                        .username( bean.getManifest().getDockerUsername())
                        .password(getDockerPassword(bean.getManifest()))
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
                .services(bean.getManifest().getServices())
                .build();
    }

    private String getDockerPassword(ApplicationManifestBean bean) {
        if (bean.getDockerImage() == null || bean.getDockerUsername() == null) {
            return null;
        }

        //TODO: Maybe outsource retrieving env variables to a dedicated class in a future feature.
        String password = System.getenv(DOCKER_PASSWORD_VAR_NAME);
        if (password == null) {
            throw new NullPointerException("Docker password is not set in environment variable: "
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

}
