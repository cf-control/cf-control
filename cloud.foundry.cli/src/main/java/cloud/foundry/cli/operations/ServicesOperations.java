package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
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
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceKey;
import org.cloudfoundry.operations.services.UnbindRouteServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;

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
            .flatMap(this::doGetServiceInstance)
            .collectMap(ServiceInstance::getName, ServiceBean::new);
    }

    private Mono<ServiceInstance> doGetServiceInstance(ServiceInstanceSummary serviceInstanceSummary) {
        return this.cloudFoundryOperations
            .services()
            .getInstance(GetServiceInstanceRequest
                .builder()
                .name(serviceInstanceSummary.getName())
                .build());
    }

    /**
     * Creates a new service in the space and binds apps to it. In case of an error,
     * the creation- and binding-process is discontinued.
     *
     * @param serviceBean serves as template for the service to create
     * @throws CreationException when the creation or the binding was not successful
     */
    public void create(String serviceInstanceName, ServiceBean serviceBean) throws CreationException {
        Log.debug("Create service:", serviceInstanceName);
        Log.debug("Bean of the service:", serviceBean);

        CreateServiceInstanceRequest createServiceRequest = CreateServiceInstanceRequest.builder()
            .serviceName(serviceBean.getService())
            .serviceInstanceName(serviceInstanceName)
            .planName(serviceBean.getPlan())
            .tags(serviceBean.getTags())
            .build();

        Mono<Void> created = this.cloudFoundryOperations.services().createInstance(createServiceRequest);

        try {
            created.block();
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
        Log.debug("Rename service:", currentName);
        Log.debug("With new name:", newName);

        RenameServiceInstanceRequest renameServiceInstanceRequest = RenameServiceInstanceRequest.builder()
            .name(currentName)
            .newName(newName)
            .build();

        try {
            this.cloudFoundryOperations.services()
                .renameInstance(renameServiceInstanceRequest)
                .block();
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
        Log.debug("Update service Instance:", serviceInstanceName);
        Log.debug("With the bean:", serviceBean);

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
     * Deletes all keys and unbinds all routes and applications associated with the
     * <code>serviceInstanceName</code>.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     */
    public void removeServiceInstance(String serviceInstanceName) {
        try {

            ServiceInstance serviceInstance = this.cloudFoundryOperations
                .services()
                .getInstance(
                    GetServiceInstanceRequest
                        .builder()
                        .name(serviceInstanceName)
                        .build())
                .block();

            // unbind route
            unbindRoute(serviceInstanceName);

            // unbind apps
            unbindApps(serviceInstanceName, serviceInstance);

            // delete keys
            deleteKeys(serviceInstanceName, serviceInstance);

            // do delete service instance
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
     * Delete service keys.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param serviceInstance     A service instance.
     */
    private void deleteKeys(String serviceInstanceName, ServiceInstance serviceInstance) {
        if (!serviceInstance.getType().getValue().equals(USER_PROVIDED_SERVICE_INSTANCE)) {
            ListServiceKeysRequest listServiceKeysRequest = ListServiceKeysRequest
                .builder()
                .serviceInstanceName(serviceInstanceName)
                .build();

            try {
                this.cloudFoundryOperations
                    .services()
                    .listServiceKeys(listServiceKeysRequest)
                    .flatMap(key -> doDeleteKey(serviceInstanceName, key))
                    .collectList()
                    .block();
                Log.info("All service keys of service instance " + serviceInstanceName + " have been deleted.");
          
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    private Mono<Void> doDeleteKey(String serviceInstanceName, ServiceKey key) {
        return this.cloudFoundryOperations
            .services()
            .deleteServiceKey(DeleteServiceKeyRequest.builder()
                .serviceInstanceName(serviceInstanceName)
                .serviceKeyName(key.getName())
                .build());
    }

    /**
     * Unbind a service instance from all applications.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @param serviceInstance     A service instance.
     */
    private void unbindApps(String serviceInstanceName, ServiceInstance serviceInstance) {
        if (serviceInstance.getApplications() == null || serviceInstance.getApplications().isEmpty()) {
            Log.info("There is no application to unbind!");
        } else {
            try {
                serviceInstance.getApplications()
                    .parallelStream()
                    .forEach(applicationName -> doUnbindApp(serviceInstanceName, applicationName));

                Log.info("All applications of service instance " + serviceInstanceName + " have been unbound.");
            
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    private void doUnbindApp(String serviceInstanceName, String applicationName) {
        try {
            this.cloudFoundryOperations
                .services()
                .unbind(
                    UnbindServiceInstanceRequest.builder()
                        .serviceInstanceName(serviceInstanceName)
                        .applicationName(applicationName)
                        .build())
                .block();
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    /**
     * Unbinds a service instance <code>serviceInstanceName</code> from all routes.
     *
     * @param serviceInstanceName serviceInstanceName Name of a service instance.
     * @return
     */
    private void unbindRoute(String serviceInstanceName) {
        ListRoutesRequest listRoutesRequest = ListRoutesRequest.builder().build();
        try {
            this.cloudFoundryOperations.routes().list(listRoutesRequest)
                .filter(route -> route.getService() != null && route.getService().equals(serviceInstanceName))
                .flatMap(this::doUnbindRoute)
                .collectList()
                .block();

            Log.info("All routes to service instance " + serviceInstanceName + " have been unbound.");

        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    private Mono<Void> doUnbindRoute(Route route) {
        return this.cloudFoundryOperations
            .services()
            .unbindRoute(
                UnbindRouteServiceInstanceRequest
                    .builder()
                    .serviceInstanceName(route.getService())
                    .domainName(route.getDomain())
                    .hostname(route.getHost())
                    .build());
    }
}
