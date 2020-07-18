package cloud.foundry.cli.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.logging.Log;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.services.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * Handles the operations for querying and manipulating services on a cloud
 * foundry instance.
 *
 * To retrieve the data from resulting Mono or Flux objects you can use
 * subscription methods (block, subscribe, etc.) provided by the reactor
 * library. For more details on how to work with Mono's visit:
 * https://projectreactor.io/docs/core/release/reference/index.html#core-features
 */
public class ServicesOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    private static final Log log = Log.getLog(ServicesOperations.class);

    private static final String USER_PROVIDED_SERVICE_INSTANCE = "user_provided_service_instance";

    public ServicesOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * Prepares a request for fetching services data from the cloud foundry
     * instance. The resulting mono will not perform any logging by default.
     *
     * @return mono object of all services as map of the service names as key and
     *         ServiceBeans as value
     */
    public Mono<Map<String, ServiceBean>> getAll() {
        return this.cloudFoundryOperations.services()
            .listInstances()
            .flatMap(serviceInstanceSummary -> getServiceInstance(serviceInstanceSummary.getName()))
            .collectMap(ServiceInstance::getName, ServiceBean::new);
    }

    private Mono<ServiceInstance> getServiceInstance(String serviceName) {
        return this.cloudFoundryOperations
            .services()
            .getInstance(createGetServiceInstanceRequest(serviceName));
    }

    private GetServiceInstanceRequest createGetServiceInstanceRequest(String serviceName) {
        return GetServiceInstanceRequest
            .builder()
            .name(serviceName)
            .build();
    }

    /**
     * Prepares a request for creating a new service with specific tags, plan and
     * parameters in the space. The resulting mono is preconfigured such that it
     * will perform logging.
     *
     * @param serviceBean serves as template for the service to create
     * @return mono which can be subscribed on to trigger the creation request to
     *         the cf instance
     * @throws NullPointerException when one of the arguments was null
     */
    public Mono<Void> create(String serviceInstanceName, ServiceBean serviceBean) {
        checkNotNull(serviceInstanceName);
        checkNotNull(serviceBean);
        CreateServiceInstanceRequest createServiceRequest = CreateServiceInstanceRequest.builder()
            .serviceName(serviceBean.getService())
            .serviceInstanceName(serviceInstanceName)
            .planName(serviceBean.getPlan())
            .tags(serviceBean.getTags())
            .parameters(serviceBean.getParams())
            .build();

        return this.cloudFoundryOperations.services().createInstance(createServiceRequest)
            .doOnSubscribe(aVoid -> {
                log.debug("Create service:", serviceInstanceName);
                log.debug("Bean of the service:", serviceBean);
            })
            .doOnSuccess(aVoid -> log.info("Service created:", serviceInstanceName))
            .onErrorStop();
    }

    /**
     * Prepares a request for renaming a service instance. The resulting mono is
     * preconfigured such that it will perform logging.
     *
     * @param newName     new name of the service instance
     * @param currentName current name of the service instance
     * @return mono which can be subscribed on to trigger the renaming request to
     *         the cf instance
     * @throws NullPointerException when one of the arguments was null
     */
    public Mono<Void> rename(String newName, String currentName) {
        checkNotNull(newName);
        checkNotNull(currentName);

        RenameServiceInstanceRequest renameServiceInstanceRequest = RenameServiceInstanceRequest.builder()
            .name(currentName)
            .newName(newName)
            .build();

        // TODO: moove logs
        return this.cloudFoundryOperations.services()
            .renameInstance(renameServiceInstanceRequest)
            .doOnSubscribe(aVoid -> {
                log.debug("Rename service:", currentName);
                log.debug("With new name:", newName);
            })
            .doOnSuccess(aVoid -> log.info("Service renamed from", currentName, "to", newName))
            .onErrorStop();
    }

    /**
     * Prepares a request for updating tags and the plan of a service instance The
     * resulting mono is preconfigured such that it will perform logging.
     *
     * @param serviceInstanceName name of a service instance
     * @param serviceBean         serves as template for the service to update
     * @return mono which can be subscribed on to trigger the update request to the
     *         cf instance
     * @throws NullPointerException when one of the arguments was null
     */
    public Mono<Void> update(String serviceInstanceName, ServiceBean serviceBean) {
        checkNotNull(serviceInstanceName);
        checkNotNull(serviceBean);
        
        return remove(serviceInstanceName).
            concatWith(create(serviceInstanceName, serviceBean)).
            then();
    }

    /**
     * Prepares a request for deleting all keys, unbinding all routes and
     * applications of a service instance and then deleting that service instance.
     * The resulting mono is preconfigured such that it will perform logging.
     *
     * @param serviceInstanceName name of the service instance to remove
     * @return mono which can be subscribed on to trigger the service deletion
     * @throws NullPointerException when the argument was null
     * @throws UpdateException      when a non recoverable error occurred
     */
    public Mono<Void> remove(String serviceInstanceName) {
        checkNotNull(serviceInstanceName);
        try {
            return unbindApps(serviceInstanceName)
                // and unbind keys
                .mergeWith(deleteKeys(serviceInstanceName))
                // also unbind routes
                .mergeWith(unbindRoutes(serviceInstanceName))
                // after previous operations are done delete the actual service
                .then(deleteServiceInstance(serviceInstanceName));
        } catch (RuntimeException e) {
            throw new UpdateException(e);
        }
    }

    private Mono<Void> deleteServiceInstance(String serviceInstanceName) {
        return this.cloudFoundryOperations.services()
            .deleteInstance(createDeleteServiceInstanceRequest(serviceInstanceName))
            .doOnSuccess(aVoid -> log.info("Service " + serviceInstanceName + " has been removed."));
    }

    private DeleteServiceInstanceRequest createDeleteServiceInstanceRequest(String serviceInstanceName) {
        return DeleteServiceInstanceRequest
            .builder()
            .name(serviceInstanceName)
            .build();
    }

    /**
     * Prepares a request for deleting keys of a service. The resulting flux is
     * preconfigured such that it will perform logging.
     *
     * @param serviceInstanceName name of the service instance to delete the keys
     *                            from
     * @return flux which can be subscribed on to delete the keys
     * @throws NullPointerException when the argument was null
     */
    public Flux<Void> deleteKeys(String serviceInstanceName) {
        checkNotNull(serviceInstanceName);

        return getServiceInstance(serviceInstanceName)
            .filter(serviceInstance -> !serviceInstance.getType()
                .getValue()
                .equals(USER_PROVIDED_SERVICE_INSTANCE))
            .hasElement()
            .flatMapMany(aBoolean -> aBoolean
                ? cloudFoundryOperations
                    .services()
                    .listServiceKeys(createListServiceKeysRequest(serviceInstanceName))
                    .doOnComplete(() -> log.info("All service keys of service instance "
                        + serviceInstanceName + " have been deleted."))
                : Flux.empty())
            .doOnComplete(() -> log.verbose("There were no keys to delete."))
            .flatMap(key -> doDeleteKey(serviceInstanceName, key));
    }

    private ListServiceKeysRequest createListServiceKeysRequest(String serviceInstanceName) {
        return ListServiceKeysRequest
            .builder()
            .serviceInstanceName(serviceInstanceName)
            .build();
    }

    private Mono<Void> doDeleteKey(String serviceInstanceName, ServiceKey key) {
        return this.cloudFoundryOperations
            .services()
            .deleteServiceKey(createDeleteServiceKeyRequest(serviceInstanceName, key))
            .doOnSubscribe(subscription -> log.info("Deleting key " + key + " for " + serviceInstanceName))
            .doOnSuccess(aVoid -> log.info("Deleted key " + key + " for service " + serviceInstanceName))
            .onErrorStop();
    }

    private DeleteServiceKeyRequest createDeleteServiceKeyRequest(String serviceInstanceName, ServiceKey key) {
        return DeleteServiceKeyRequest.builder()
            .serviceInstanceName(serviceInstanceName)
            .serviceKeyName(key.getName())
            .build();
    }

    /**
     * Prepares a request for unbinding a service instance from all its
     * applications. The resulting flux is preconfigured such that it will perform
     * logging.
     *
     * @param serviceInstanceName name of the service instance to unbind the
     *                            applications from
     * @return flux which can be subscribed on to unbind the applications
     */
    public Flux<Void> unbindApps(String serviceInstanceName) {
        checkNotNull(serviceInstanceName);

        return getServiceInstance(serviceInstanceName)
            .flatMapIterable(serviceInstance -> {
                if (serviceInstance.getApplications() == null || serviceInstance.getApplications().isEmpty()) {
                    log.verbose("There is no application to unbind!");
                    return Collections.emptyList();
                } else {
                    return serviceInstance.getApplications();
                }
            })
            .flatMap(appName -> unbindApp(serviceInstanceName, appName))
            .doOnComplete(() -> log.info("All applications of service instance "
                + serviceInstanceName + " have been unbound."));
    }

    /**
     * Prepares a request for unbinding a service instance from an application. The
     * resulting mono is preconfigured such that it will perform logging.
     *
     * @param serviceInstanceName name of the service instance to unbind the
     *                            application from
     * @param applicationName     name of the application
     * @return mono which can be subscribed on to unbind the application
     */
    public Mono<Void> unbindApp(String serviceInstanceName, String applicationName) {
        checkNotNull(serviceInstanceName);
        checkNotNull(applicationName);

        return this.cloudFoundryOperations
            .services()
            .unbind(createUnbindServiceInstanceRequest(serviceInstanceName, applicationName))
            .doOnSubscribe(subscription -> log.info("Unbind app " + applicationName +
                " for " + serviceInstanceName))
            .doOnSuccess(subscription -> log.info("Unbound app " + applicationName +
                " for " + serviceInstanceName))
            .onErrorStop();
    }

    private UnbindServiceInstanceRequest createUnbindServiceInstanceRequest(String serviceInstanceName,
        String applicationName) {
        return UnbindServiceInstanceRequest.builder()
            .serviceInstanceName(serviceInstanceName)
            .applicationName(applicationName)
            .build();
    }

    /**
     * Prepares a request for unbinding a service instance from all routes. The
     * resulting flux is preconfigured such that it will perform logging.
     *
     * @param serviceInstanceName name of the service instance to unbind all routes
     *                            from
     * @throws NullPointerException if the argument is null
     * @return flux which can be subscribed on to unbind the routes
     */
    public Flux<Void> unbindRoutes(String serviceInstanceName) {
        checkNotNull(serviceInstanceName);

        return this.cloudFoundryOperations.routes()
            .list(ListRoutesRequest.builder().build())
            .filter(route -> route.getService() != null && route.getService().equals(serviceInstanceName))
            .flatMap(this::doUnbindRoute)
            .doOnComplete(() -> log.info("All routes to service instance "
                + serviceInstanceName + " have been unbound."));
    }

    private Mono<Void> doUnbindRoute(Route route) {
        return this.cloudFoundryOperations
            .services()
            .unbindRoute(createRouteServiceInstanceRequest(route))
            .doOnSubscribe(subscription -> log.info("unbind route " + route))
            .onErrorStop();
    }

    private UnbindRouteServiceInstanceRequest createRouteServiceInstanceRequest(Route route) {
        return UnbindRouteServiceInstanceRequest
            .builder()
            .serviceInstanceName(route.getService())
            .domainName(route.getDomain())
            .hostname(route.getHost())
            .build();
    }

}
