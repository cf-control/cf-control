package cloud.foundry.cli.operations;

import static cloud.foundry.cli.operations.RouteUtils.decomposeRoute;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import org.cloudfoundry.client.v3.*;
import org.cloudfoundry.client.v3.applications.*;

import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import org.cloudfoundry.operations.applications.*;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Route;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.routes.*;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the operations for querying and manipulating applications on a cloud
 * foundry instance.
 *
 * To retrieve the data from resulting Mono or Flux objects you can use
 * subscription methods (block, subscribe, etc.) provided by the reactor
 * library. For more details on how to work with Mono's visit:
 * https://projectreactor.io/docs/core/release/reference/index.html#core-features
 */
public class ApplicationsOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    private static final Log log = Log.getLog(ApplicationsOperations.class);

    private boolean autoStart;

    /**
     * Sets auto start of the apps to true by default
     * @param cloudFoundryOperations the cloud foundry operations instance
     */
    public ApplicationsOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        this(cloudFoundryOperations, true);
    }

    /**
     * @param cloudFoundryOperations the cloud foundry operations instance
     * @param autoStart sets whether app should be started when deployed
     */
    public ApplicationsOperations(DefaultCloudFoundryOperations cloudFoundryOperations, boolean autoStart) {
        super(cloudFoundryOperations);
        this.autoStart = autoStart;
    }

    /**
     * Prepares a request for fetching applications data from the cloud foundry
     * instance. The resulting mono will not perform any logging by default.
     *
     * @return mono object of all applications as map of the application names as
     *         key and the ApplicationBeans as value
     */
    public Mono<Map<String, ApplicationBean>> getAll() {
        return this.cloudFoundryOperations
            .applications()
            .list()
            // group the application and the metadata in pairs
            .flatMap(applicationSummary -> Flux.zip(
                getApplicationManifest(applicationSummary),
                getMetadata(applicationSummary)))
            // T1 is the ApplicationManifest and T2 is the metadata of the application
            .collectMap(tuple -> tuple.getT1().getName(),
                tuple -> new ApplicationBean(tuple.getT1(), tuple.getT2()));
    }

    private Mono<ApplicationManifest> getApplicationManifest(ApplicationSummary applicationSummary) {
        return this.cloudFoundryOperations
            .applications()
            .getApplicationManifest(GetApplicationManifestRequest
                .builder()
                .name(applicationSummary.getName())
                .build());
    }

    private Mono<Metadata> getMetadata(ApplicationSummary applicationSummary) {
        GetApplicationRequest request = GetApplicationRequest.builder()
            .applicationId(applicationSummary.getId())
            .build();
        return this.cloudFoundryOperations.getCloudFoundryClient()
            .applicationsV3()
            .get(request)
            .map(GetApplicationResponse::getMetadata);
    }

    /**
     * Prepares a request for deleting a specific application associated with the
     * provided name. The resulting mono is preconfigured such that it will perform
     * logging.
     *
     * @param applicationName applicationName Name of an application.
     * @throws NullPointerException when the applicationName is null
     * @return mono which can be subscribed on to trigger the removal of the app.
     *         The mono also handles the logging.
     */
    public Mono<Void> remove(String applicationName) {
        checkNotNull(applicationName);

        DeleteApplicationRequest request = DeleteApplicationRequest
            .builder()
            .name(applicationName)
            .build();

        return this.cloudFoundryOperations.applications()
            .delete(request)
            .doOnSuccess(aVoid -> log.info("App removed: ", applicationName))
            .onErrorStop();
    }

    /**
     * Prepares a request for updating the app to the cloud foundry instance specified
     * within the cloud foundry operations instance. The resulting mono is
     * preconfigured such that it will perform logging.
     * Currently implemented by removing the app and afterwards newly creating it.
     *
     * @param appName     name of the application
     * @param bean        application bean that holds the configuration settings to
     *                    deploy the app to the cloud foundry instance
     * @throws NullPointerException     when bean or app name is null or docker
     *                                  password was not set in environment
     *                                  variables when creating app via dockerImage
     *                                  and docker credentials
     * @throws IllegalArgumentException when app name empty
     * @throws CreationException        when any fatal error occurs during creation
     *                                  of the app
     * @return mono which can be subscribed on to trigger the creation of the app
     */
    public Mono<Void> update(String appName, ApplicationBean bean) {
        return this.remove(appName)
                .then(this.create(appName, bean));
    }


    /**
     * Prepares a request for pushing an app to the cloud foundry instance specified
     * within the cloud foundry operations instance. The resulting mono is
     * preconfigured such that it will perform logging.
     *
     * @param appName     name of the application
     * @param bean        application bean that holds the configuration settings to
     *                    deploy the app to the cloud foundry instance
     * @throws NullPointerException     when bean or app name is null or docker
     *                                  password was not set in environment
     *                                  variables when creating app via dockerImage
     *                                  and docker credentials
     * @throws IllegalArgumentException when app name empty
     * @throws CreationException        when any fatal error occurs during creation
     *                                  of the app
     * @return mono which can be subscribed on to trigger the creation of the app
     */
    public Mono<Void> create(String appName, ApplicationBean bean) {
        checkNotNull(appName, "Application name cannot be null");
        checkArgument(!appName.isEmpty(), "Application name cannot be empty");
        checkNotNull(bean, "Application contents cannot be null");

        try {
            return doCreate(appName, bean);
        } catch (RuntimeException e) {
            throw new CreationException(e);
        }
    }

    private Mono<Void> doCreate(String appName, ApplicationBean bean) {
        return this.cloudFoundryOperations
                .getSpaceId()
                .flatMap(spaceId -> this.cloudFoundryOperations
                        .getCloudFoundryClient()
                        .applicationsV3()
                        .create(buildCreateApplicationRequest(spaceId, appName, bean))
                        .doOnSubscribe(subscription -> {
                            log.debug("Create app:", appName);
                            log.debug("Bean of the app:", bean);
                            log.debug("Should the app start:", this.autoStart);
                        })
                        .doOnSuccess(aVoid -> log.info("App created:", appName))
                        .then())
                .then(this.cloudFoundryOperations
                .applications()
                .pushManifest(PushApplicationManifestRequest
                        .builder()
                        .manifest(buildApplicationManifest(appName, bean))
                        .noStart(!this.autoStart)
                        .build())
                .onErrorContinue(this::whenServiceNotFound, log::warning)
                .doOnSubscribe(subscription -> log.debug("Pushing app manifest for:", appName))
                .doOnSuccess(aVoid -> log.info("App manifest pushed for:", appName)));

    }

    private boolean whenServiceNotFound(Throwable throwable) {
        return throwable instanceof IllegalArgumentException
            && throwable.getMessage().contains("Service instance")
            && throwable.getMessage().contains("could not be found");
    }

    private CreateApplicationRequest buildCreateApplicationRequest(String spaceId,
                                                                   String appName,
                                                                   ApplicationBean bean) {
        Map<String,? extends String> envVars = Collections.emptyMap();
        if (bean.getManifest() != null && bean.getManifest().getEnvironmentVariables() != null) {
            envVars = bean.getManifest().getEnvironmentVariables().entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
        }

        ToOneRelationship spaceRelationship = ToOneRelationship.builder()
                .data(Relationship.builder().id(spaceId).build())
                .build();

        Lifecycle lifecycle = null;
        if (bean.getPath() != null || bean.getManifest() != null) {
            lifecycle = Lifecycle.builder()
                    .type(LifecycleType.BUILDPACK)
                    .data(BuildpackData.builder().buildpacks(bean.getManifest().getBuildpack()).build())
                    .build();
        }

        return CreateApplicationRequest.builder()
                .environmentVariables(envVars)
                .lifecycle(lifecycle)
                .metadata(Metadata.builder()
                        .annotation(ApplicationBean.METADATA_KEY, bean.getMeta())
                        .annotation(ApplicationBean.PATH_KEY, bean.getPath())
                        .build())
                .name(appName)
                .relationships(ApplicationRelationships.builder().space(spaceRelationship).build())
                .build();
    }

    private ApplicationManifest buildApplicationManifest(String appName, ApplicationBean bean) {
        if (bean.getManifest() == null) {
            bean.setManifest(new ApplicationManifestBean());
        }

        boolean noRoute = false;
        if (bean.getManifest().getRandomRoute() == null || !bean.getManifest().getRandomRoute()) {
            if (bean.getManifest().getRoutes() == null || bean.getManifest().getRoutes().size() == 0) {
                noRoute = true;
            }
        }

        return ApplicationManifest.builder()
                .name(appName)
                .path(bean.getPath() != null ? Paths.get(bean.getPath()) : Paths.get(""))
                .buildpack(bean.getManifest().getBuildpack())
                .command(bean.getManifest().getCommand())
                .disk(bean.getManifest().getDisk())
                .healthCheckHttpEndpoint(bean.getManifest().getHealthCheckHttpEndpoint())
                .healthCheckType(bean.getManifest().getHealthCheckType())
                .instances(bean.getManifest().getInstances())
                .memory(bean.getManifest().getMemory())
                .noRoute(noRoute)
                .randomRoute(bean.getManifest().getRandomRoute())
                .routes(getAppRoutes(bean.getManifest().getRoutes()))
                .stack(bean.getManifest().getStack())
                .timeout(bean.getManifest().getTimeout())
                .services(bean.getManifest().getServices())
                .build();
    }

    private List<Route> getAppRoutes(List<String> routes) {
        return routes == null ? null
            : routes
                .stream()
                .filter(Objects::nonNull)
                .map(route -> Route.builder().route(route).build())
                .collect(Collectors.toList());
    }

    /**
     * Prepares a request for renaming an application instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param newName     new name of the application instance
     * @param currentName current name of the application instance
     * @return mono which can be subscribed on to trigger the renaming request to the cf instance
     * @throws NullPointerException when one of the arguments was null
     */
    public Mono<Void> rename(String newName, String currentName) {
        checkNotNull(newName);
        checkNotNull(currentName);

        RenameApplicationRequest renameApplicationRequest = RenameApplicationRequest.builder()
                .name(currentName)
                .newName(newName)
                .build();

        return this.cloudFoundryOperations.applications().rename(renameApplicationRequest)
                .doOnSubscribe(aVoid -> {
                    log.debug("Rename application:", currentName);
                    log.debug("With new name:", newName); })
                .doOnSuccess(aVoid -> log.info("Application renamed from", currentName, "to", newName));
    }

    /**
     * Prepares a request for scaling properties of an application instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the name of the application to scale
     * @param diskLimit the new disk limit
     * @param memoryLimit the new memory limit
     * @param instances the new number of instances
     * @return mono which can be subscribed on to trigger the scale request to the cf instance
     * @throws NullPointerException if the provided application name is null
     */
    public Mono<Void> scale(String applicationName, Integer diskLimit, Integer memoryLimit, Integer instances) {
        checkNotNull(applicationName);

        ScaleApplicationRequest scaleRequest = ScaleApplicationRequest.builder()
                .name(applicationName)
                .diskLimit(diskLimit)
                .memoryLimit(memoryLimit)
                .instances(instances)
                .build();

        return cloudFoundryOperations.applications().scale(scaleRequest)
                .doOnSubscribe(aVoid -> {
                    log.debug("Scale app:", applicationName);
                    log.debug("With new disk limit:", diskLimit);
                    log.debug("With new memory limit:", memoryLimit);
                    log.debug("With new number of instances:", instances); })
                .doOnSuccess(aVoid -> log.info("Application", applicationName, "was scaled"))
                .onErrorStop()
                .then();
    }

    /**
     * Prepares a request for adding an environment variable to an application instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the name of the application to add the environment variable for
     * @param variableName the name of the environment variable to add
     * @param variableValue the value of the environment variable to add
     * @return mono which can be subscribed on to trigger the environment variable request to the cf instance
     * @throws NullPointerException if any of the arguments are null
     */
    public Mono<Void> addEnvironmentVariable(String applicationName, String variableName, String variableValue) {
        checkNotNull(applicationName);
        checkNotNull(variableName);
        checkNotNull(variableValue);

        SetEnvironmentVariableApplicationRequest addEnvVarRequest = SetEnvironmentVariableApplicationRequest.builder()
                .name(applicationName)
                .variableName(variableName)
                .variableValue(variableValue)
                .build();

        return cloudFoundryOperations.applications().setEnvironmentVariable(addEnvVarRequest)
                .doOnSubscribe(aVoid -> {
                    log.debug("Adding environment variable",
                            variableName,
                            "with value",
                            variableValue,
                            "for app:",
                            applicationName); })
                .doOnSuccess(aVoid -> log.info("Environment variable",
                        variableName,
                        "with value",
                        variableValue ,
                        "was added to the app",
                        applicationName));
    }

    /**
     * Prepares a request for removing an environment variable from an application instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the name of the application to remove the environment variable of
     * @param variableName the name of the environment variable to remove
     * @return mono which can be subscribed on to trigger the environment variable request to the cf instance
     * @throws NullPointerException if any of the arguments are null
     */
    public Mono<Void> removeEnvironmentVariable(String applicationName, String variableName) {
        checkNotNull(applicationName);
        checkNotNull(variableName);

        UnsetEnvironmentVariableApplicationRequest removeEnvVarRequest = UnsetEnvironmentVariableApplicationRequest
                .builder()
                .name(applicationName)
                .variableName(variableName)
                .build();

        return cloudFoundryOperations.applications().unsetEnvironmentVariable(removeEnvVarRequest)
                .doOnSubscribe(aVoid -> log.debug("Removing environment variable",
                        variableName,
                        "for app:",
                        applicationName))
                .doOnSuccess(aVoid -> log.info("Environment variable",
                        variableName,
                            "was removed from the app",
                        applicationName));
    }

    /**
     * Prepares a request for setting the type of the health check of an application instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the name of the application to set the health check type of
     * @param healthCheckType the health check type to set
     * @return mono which can be subscribed on to trigger the health check type request to the cf instance
     * @throws NullPointerException if any of the arguments are null
     */
    public Mono<Void> setHealthCheck(String applicationName, ApplicationHealthCheck healthCheckType) {
        checkNotNull(applicationName);
        checkNotNull(healthCheckType);

        SetApplicationHealthCheckRequest setHealthCheckRequest = SetApplicationHealthCheckRequest.builder()
                .name(applicationName)
                .type(healthCheckType)
                .build();

        return cloudFoundryOperations.applications().setHealthCheck(setHealthCheckRequest)
                .doOnSubscribe(aVoid -> {
                    log.debug("Set health check type for app:", applicationName);
                    log.debug("With health check type:", healthCheckType); })
                .doOnSuccess(aVoid -> log.info("The health check type of the app", applicationName,
                        "was set to", healthCheckType));
    }

    /**
     * Prepares a request for binding an app to a service.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the app that should be bound to the service
     * @param serviceName the service to which the app should be bound
     * @return mono which can be subscribed on to trigger the app binding
     * @throws NullPointerException if any of the arguments is null
     */
    public Mono<Void> bindToService(String applicationName, String serviceName) {
        checkNotNull(applicationName);
        checkNotNull(serviceName);

        BindServiceInstanceRequest bindServiceRequest = BindServiceInstanceRequest.builder()
                .applicationName(applicationName)
                .serviceInstanceName(serviceName)
                .build();

        return cloudFoundryOperations.services().bind(bindServiceRequest)
                .doOnSuccess(aVoid -> {
                    log.debug("Bind app:", applicationName);
                    log.debug("To service:", serviceName); })
                .doOnSuccess(aVoid -> log.info("Bound the app", applicationName,
                        "to the service", serviceName));
    }

    /**
     * Prepares a request for unbinding an app from a service.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the app that should be unbound from the service
     * @param serviceName the service from which the app should be unbound
     * @return mono which can be subscribed on to trigger the app unbinding
     * @throws NullPointerException if any of the arguments is null
     */
    public Mono<Void> unbindFromService(String applicationName, String serviceName) {
        checkNotNull(applicationName);
        checkNotNull(serviceName);

        UnbindServiceInstanceRequest unbindServiceRequest = UnbindServiceInstanceRequest.builder()
                .applicationName(applicationName)
                .serviceInstanceName(serviceName)
                .build();

        return cloudFoundryOperations.services().unbind(unbindServiceRequest)
                .doOnSuccess(aVoid -> {
                    log.debug("Unbind app:", applicationName);
                    log.debug("From service:", serviceName); })
                .doOnSuccess(aVoid -> log.info("Unbound the app", applicationName,
                        "from the service", serviceName));
    }

    /**
     * Prepares a request for adding a route to an app.
     * This process will create the route.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the app to which the route should be added
     * @param route the route to be added from the app
     * @return mono which can be subscribed on to trigger the route addition
     * @throws NullPointerException if any of the arguments is null
     */
    public Mono<Void> addRoute(String applicationName, String route) {
        checkNotNull(applicationName);
        checkNotNull(route);

        return this.cloudFoundryOperations
                .domains()
                .list()
                .map(this::createDomainSummary)
                .collectList()
                .flatMap(domainSummaries -> decomposeRoute(domainSummaries, route, route))
                .flatMap(decomposedRoute -> cloudFoundryOperations.routes().map(MapRouteRequest.builder()
                        .applicationName(applicationName)
                        .domain(decomposedRoute.getDomain())
                        .host(decomposedRoute.getHost())
                        .path(decomposedRoute.getPath())
                        .build())
                        .doOnSuccess(aVoid -> {
                            log.debug("Add route:", route);
                            log.debug("To app:", applicationName); })
                        .doOnSuccess(aVoid -> log.info("Added the route", route,
                                "to the application", applicationName)))
                .onErrorStop()
                .then();
    }

    /**
     * Prepares a request for removing a route of an app.
     * This process will also remove the route itself.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param applicationName the app of which the route should be removed
     * @param route the route to be removed from the app
     * @return mono which can be subscribed on to trigger the route removal
     * @throws NullPointerException if any of the arguments is null
     */
    public Mono<Void> removeRoute(String applicationName, String route) {
        checkNotNull(applicationName);
        checkNotNull(route);

        return this.cloudFoundryOperations
                .domains()
                .list()
                .map(this::createDomainSummary)
                .collectList()
                .flatMap(domainSummaries -> decomposeRoute(domainSummaries, route, route))
                .flatMap(decomposedRoute -> cloudFoundryOperations.routes().unmap(UnmapRouteRequest.builder()
                        .applicationName(applicationName)
                        .domain(decomposedRoute.getDomain())
                        .path(decomposedRoute.getPath())
                        .host(decomposedRoute.getHost())
                        .build())
                        .doOnSuccess(aVoid -> {
                            log.debug("Remove route:", route);
                            log.debug("From app:", applicationName); })
                        .doOnSuccess(aVoid -> log.info("Removed the route", route,
                                "from the application", applicationName)));
    }

    private DomainSummary createDomainSummary(Domain domain) {
        return DomainSummary
                .builder()
                .id(domain.getId())
                .name(domain.getName())
                .type(domain.getType())
                .build();
    }

}
