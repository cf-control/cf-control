package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;

import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceResponse;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceKeysRequest;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceKeysResponse;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyResource;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.GetUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.GetUserProvidedServiceInstanceRequest.Builder;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.GetUserProvidedServiceInstanceResponse;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstancesRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstancesResponse;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.UserProvidedServiceInstanceResource;
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
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.services.UnbindRouteServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;

import reactor.core.publisher.Mono;

import java.util.List;
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
        return this.cloudFoundryOperations
                .services()
                .listInstances()
                .collectMap(ServiceInstanceSummary::getName, ServiceBean::new);
    }

    /**
     * Creates a new service in the space and binds apps to it. In case of an error,
     * the creation- and binding-process is discontinued.
     *
     * @param serviceBean serves as template for the service to create
     * @throws CreationException when the creation or the binding was not successful
     */
    public void create(String serviceInstanceName, ServiceBean serviceBean) throws CreationException {
        CreateServiceInstanceRequest createServiceRequest = CreateServiceInstanceRequest.builder()
                .serviceName(serviceBean.getService())
                .serviceInstanceName(serviceInstanceName)
                .planName(serviceBean.getPlan())
                .tags(serviceBean.getTags())
                .build();

        Mono<Void> created = this.cloudFoundryOperations.services().createInstance(createServiceRequest);

        try {
            created.block();
            Log.info("Service \"" + serviceInstanceName + "\" has been created.");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }

    }

    /**
     * Rename a service instance
     *
     * @param currentName Current Name of the Service Instance
     * @param newName     New Name of the Service Instance
     * @throws CreationException when the creation or the binding was not successful
     */
    public void renameServiceInstance(String newName, String currentName) throws CreationException {
        RenameServiceInstanceRequest renameServiceInstanceRequest = RenameServiceInstanceRequest.builder()
                .name(currentName)
                .newName(newName)
                .build();

        try {
            this.cloudFoundryOperations.services()
                    .renameInstance(renameServiceInstanceRequest)
                    .block();
            Log.info("Name of Service Instance has been changed");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }
    }

    /**
     * Update Tags, Plan of a Service Instance
     *
     * @param serviceInstanceName Name of a service instance
     * @param serviceBean         serves as template for the service to update
     * @throws CreationException when the creation or the binding was not successful
     */
    public void updateServiceInstance(String serviceInstanceName, ServiceBean serviceBean) throws CreationException {
        UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .tags(serviceBean.getTags())
                .planName(serviceBean.getPlan())
                .build();

        try {
            this.cloudFoundryOperations.services()
                    .updateInstance(updateServiceInstanceRequest)
                    .block();
            Log.info("Service Plan and Tags haven been updated");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }
    }

    /**
     * Deletes all keys and unbinds all routes and applications associated with the <code>serviceInstanceName</code>.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     */
    public void removeServiceInstance(String serviceInstanceName) {
        try {
            // unbind route
            System.out.print(" ROUTE");
            unbindRoute(serviceInstanceName);

            // unbind apps
            System.out.print(" APPS ");
            GetServiceInstanceRequest getServiceInstanceRequest = GetServiceInstanceRequest
                    .builder()
                    .name(serviceInstanceName)
                    .build();

            ServiceInstance serviceInstance = this.cloudFoundryOperations
                    .services()
                    .getInstance(getServiceInstanceRequest)
                    .block();

            unbindApps(serviceInstanceName, serviceInstance);

            // delete keys
            System.out.print("KEYS");
            deleteKeys(serviceInstanceName, serviceInstance);

            DeleteServiceInstanceRequest deleteServiceInstanceRequest = DeleteServiceInstanceRequest
                    .builder()
                    .name(serviceInstanceName)
                    .build();

            this.cloudFoundryOperations.services()
                    .deleteInstance(deleteServiceInstanceRequest)
                    .block();

            Log.info("Service " + serviceInstanceName + " has been removed.");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Delete a service key.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param serviceInstance A service instance.
     */
    private void deleteKeys(String serviceInstanceName, ServiceInstance serviceInstance) {
        if (!serviceInstance.getType().getValue().equals(USER_PROVIDED_SERVICE_INSTANCE)) {
            ListServiceKeysRequest listServiceKeysRequest = ListServiceKeysRequest
                    .builder()
                    .serviceInstanceName(serviceInstanceName)
                    .build();

            List<ServiceKey> keys = this.cloudFoundryOperations
                    .services()
                    .listServiceKeys(listServiceKeysRequest)
                    .collectList()
                    .block();

            if (keys != null && !keys.isEmpty()) {
                keys.forEach(
                        key -> this.cloudFoundryOperations
                                .services()
                                .deleteServiceKey(createDeleteServiceKeyRequest(serviceInstanceName, key))
                                .block()
                );
            }
        }
    }

    /**
     * Creates an <code>delete service instance request</code>.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param key A service key.
     * @return delete service key request.
     */
    private DeleteServiceKeyRequest createDeleteServiceKeyRequest(String serviceInstanceName, ServiceKey key) {
        return DeleteServiceKeyRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .serviceKeyName(key.getName())
                .build();
    }

    /**
     * Unbind a service instance from an application.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param serviceInstance A service instance.
     */
    private void unbindApps(String serviceInstanceName, ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            Log.info("There is no application to unbind!");
        } else {
            serviceInstance.getApplications()
                    .forEach(applicationName ->
                            this.cloudFoundryOperations
                                    .services()
                                    .unbind(createUnbindServiceInstanceRequest(serviceInstanceName, applicationName))
                                    .block());
        }
    }

    /**
     * Creates an <code>unbind service instance request</code>.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param applicationName     The value for applicationName.
     * @return unbind service instance request.
     */
    private UnbindServiceInstanceRequest createUnbindServiceInstanceRequest(
            String serviceInstanceName,
            String applicationName) {
        Log.info("Unbind application " + applicationName + " for the service " + serviceInstanceName);

        return UnbindServiceInstanceRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .applicationName(applicationName)
                .build();
    }

    /**
     * Unbinds a service instance <code>serviceInstanceName</code> from the route.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     */
    private void unbindRoute(String serviceInstanceName) {
        ListRoutesRequest listRoutesRequest = ListRoutesRequest.builder().build();
        List<Route> routes = this.cloudFoundryOperations.routes().list(listRoutesRequest).collectList().block();
        if (routes == null || routes.isEmpty()) {
            Log.info("There is no route to unbind!");
        } else {
            routes.stream()
                    .filter(route ->
                            route.getService() != null && route.getService().equals(serviceInstanceName))
                    .forEach(route ->
                            this.cloudFoundryOperations
                                    .services()
                                    .unbindRoute(createUnbindRouteServiceInstanceRequest(serviceInstanceName, route))
                                    .block()
                    );
        }
    }

    /**
     * Creates an <code>unbind route service instance request</code>.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param route               A route.
     * @return unbind route service instance request.
     */
    private UnbindRouteServiceInstanceRequest createUnbindRouteServiceInstanceRequest(
            String serviceInstanceName,
            Route route) {
        Log.info("Unbind route " + route.getDomain() + " for the service " + serviceInstanceName);

        return UnbindRouteServiceInstanceRequest
                .builder()
                .serviceInstanceName(serviceInstanceName)
                .domainName(route.getDomain())
                .hostname(route.getHost())
                .build();
    }

}
