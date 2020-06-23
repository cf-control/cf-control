package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.logging.Log;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceKeyRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ListServiceKeysRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.services.UnbindRouteServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handles the operations for manipulating services on a cloud foundry instance.
 */
public class ServicesOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    private static final String USER_PROVIDED_SERVICE_INSTANCE = "user_provided_service_instance";

    public ServicesOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * This method fetches services data from the cloud foundry instance. To
     * retrieve data given by the Mono object you can use the subscription methods
     * (block, subscribe, etc.) provided by the reactor library. For more details on
     * how to work with Mono's visit:
     * https://projectreactor.io/docs/core/release/reference/index.html#core-features
     *
     * @return Mono object of all services as list of ServiceBeans
     */
    public Mono<Map<String, ServiceBean>> getAll() {
        return  this.cloudFoundryOperations.services()
                .listInstances()
                .flatMap(serviceInstanceSummary -> getServiceInstance(serviceInstanceSummary.getName()))
                .collectMap(ServiceInstance::getName, ServiceBean::new);
    }

    private Mono<ServiceInstance> getServiceInstance(String serviceName) {
        return this.cloudFoundryOperations
                .services()
                .getInstance(GetServiceInstanceRequest
                    .builder()
                    .name(serviceName)
                    .build());
    }

    /**
     * Creates a new service in the space and binds apps to it. In case of an error,
     * the creation- and binding-process is discontinued.
     *
     * @param serviceBean serves as template for the service to create
     * @return Mono which can be subscribed on to trigger the request to the cf instance
     */
    public Mono<Void> create(String serviceInstanceName, ServiceBean serviceBean) {

        CreateServiceInstanceRequest createServiceRequest = CreateServiceInstanceRequest.builder()
                .serviceName(serviceBean.getService())
                .serviceInstanceName(serviceInstanceName)
                .planName(serviceBean.getPlan())
                .tags(serviceBean.getTags())
                .build();

        return this.cloudFoundryOperations.services().createInstance(createServiceRequest)
                .doOnSubscribe(aVoid -> {
                    Log.debug("Create service:", serviceInstanceName);
                    Log.debug("Bean of the service:", serviceBean);
                })
                .doOnSuccess(aVoid -> Log.info("Service created:", serviceInstanceName));
    }

    /**
     * Rename a service instance
     *
     * @param newName     New Name of the Service Instance
     * @param currentName Current Name of the Service Instance
     * @return Mono which can be subscribed on to trigger the request to the cf instance
     */
    public Mono<Void> rename(String newName, String currentName) {
        RenameServiceInstanceRequest renameServiceInstanceRequest = RenameServiceInstanceRequest.builder()
                .name(currentName)
                .newName(newName)
                .build();

        //TODO: moove logs
        return this.cloudFoundryOperations.services()
                .renameInstance(renameServiceInstanceRequest)
                .doOnSubscribe(aVoid -> {
                    Log.debug("Rename service:", currentName);
                    Log.debug("With new name:", newName);
                });
    }

    /**
     * Update Tags, Plan of a Service Instance
     *
     * @param serviceInstanceName Name of a service instance
     * @param serviceBean         serves as template for the service to update
     * @return Mono which can be subscribed on to trigger the request to the cf instance
     */
    public Mono<Void> update(String serviceInstanceName, ServiceBean serviceBean) {
        //TODO:move logs
        UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .tags(serviceBean.getTags())
                .planName(serviceBean.getPlan())
                .build();
                return this.cloudFoundryOperations.services()
                        .updateInstance(updateServiceInstanceRequest)
                        .doOnSubscribe(subscription -> {
                            Log.debug("Update service Instance:", serviceInstanceName);
                            Log.debug("With the bean:", serviceBean);
                        });
    }

    /**
     * Deletes all keys and unbinds all routes and applications associated with the
     * <code>serviceInstanceName</code> and then deletes the service instance.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @return Mono which can be subscribed on to trigger the service deletion
     */
    public Mono<Void> remove(String serviceInstanceName) {
        try {
                    // get service instance
            return Flux.from(getServiceInstance(serviceInstanceName))
                    // with the result trigger app binding and key binding deletions
                    .flatMap(serviceInstance -> mergeUnbindAppsAndDeleteKeys(serviceInstanceName, serviceInstance))
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
                .doOnSuccess(aVoid -> Log.info("Service " + serviceInstanceName + " has been removed."));
    }

    private DeleteServiceInstanceRequest createDeleteServiceInstanceRequest(String serviceInstanceName) {
        return DeleteServiceInstanceRequest
                .builder()
                .name(serviceInstanceName)
                .build();
    }

    private Flux<Void> mergeUnbindAppsAndDeleteKeys(String serviceInstanceName, ServiceInstance servInstance) {
        return Flux.merge(unbindApps(serviceInstanceName, servInstance), deleteKeys(serviceInstanceName, servInstance));
    }

    /**
     * Delete service keys.
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param serviceInstance A service instance.
     * @return Flux which can be subscribed on, to delete the keys
     */
    private Flux<Void> deleteKeys(String serviceInstanceName, ServiceInstance serviceInstance) {
        if (!serviceInstance.getType().getValue().equals(USER_PROVIDED_SERVICE_INSTANCE)) {
            return this.cloudFoundryOperations
                    .services()
                    .listServiceKeys(createListServiceKeysRequest(serviceInstanceName))
                    .flatMap(key -> doDeleteKey(serviceInstanceName, key))
                    .doOnComplete(() -> Log.info("All service keys of service instance "
                        + serviceInstanceName + " have been deleted."));
        }
        return Flux.empty();
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
                .doOnSubscribe(subscription -> Log.info("Deleting key " + key + " for " + serviceInstanceName))
                .doOnSuccess(aVoid -> Log.info("Deleted key " + key + " for service " + serviceInstanceName));
    }

    private DeleteServiceKeyRequest createDeleteServiceKeyRequest(String serviceInstanceName, ServiceKey key) {
        return DeleteServiceKeyRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .serviceKeyName(key.getName())
                .build();
    }

    /**
     * Unbind a service instance from all applications.
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param serviceInstance     A service instance.
     * @return Flux which can be subscribed on, to unbind the apps
     */
    private Flux<Void> unbindApps(String serviceInstanceName, ServiceInstance serviceInstance) {
        if (serviceInstance.getApplications() == null || serviceInstance.getApplications().isEmpty()) {
            Log.info("There is no application to unbind!");
        } else {
                return Flux.fromIterable(serviceInstance.getApplications())
                        .flatMap(appName -> doUnbindApp(serviceInstanceName, appName))
                        .doOnComplete(() -> Log.info("All applications of service instance " + serviceInstanceName + " have been unbound."));
        }
        return null;
    }

    private Mono<Void> doUnbindApp(String serviceInstanceName, String applicationName) {
            return this.cloudFoundryOperations
                    .services()
                    .unbind(createUnbindServiceInstanceRequest(serviceInstanceName, applicationName))
                    .doOnSubscribe(subscription -> Log.info("Unbind app " + applicationName + " for " + serviceInstanceName))
                    .doOnSuccess(subscription -> Log.info("Unbound app " + applicationName + " for " + serviceInstanceName));
    }

    private UnbindServiceInstanceRequest createUnbindServiceInstanceRequest(String serviceInstanceName,
                                                                            String applicationName) {
        return UnbindServiceInstanceRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .applicationName(applicationName)
                .build();
    }

    /**
     * Unbinds a service instance <code>serviceInstanceName</code> from all routes.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @return Flux which can be subscribed on, to unbind the routes
     */
    private Flux<Void> unbindRoutes(String serviceInstanceName) {
        ListRoutesRequest listRoutesRequest = ListRoutesRequest.builder().build();
            return this.cloudFoundryOperations.routes()
                    .list(listRoutesRequest)
                    .filter(route -> route.getService() != null && route.getService().equals(serviceInstanceName))
                    .flatMap(this::doUnbindRoute)
                    .doOnComplete(() -> Log.info("All routes to service instance "
                            + serviceInstanceName + " have been unbound."));
    }

    private Mono<Void> doUnbindRoute(Route route) {
        return this.cloudFoundryOperations
                .services()
                .unbindRoute(createRouteServiceInstanceRequest(route))
                .doOnSubscribe(subscription -> Log.info("unbind route " + route));
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
