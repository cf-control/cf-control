package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.Routes;
import org.cloudfoundry.operations.services.*;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Handles the operations for manipulating services on a cloud foundry instance.
 */
public class ServicesOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public ServicesOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * This method fetches services data from the cloud foundry instance.
     * To retrieve data given by the Mono object you can use the subscription methods (block, subscribe, etc.) provided
     * by the reactor library.
     * For more details on how to work with Mono's visit:
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

    public void removeServiceInstance(String serviceInstanceName) throws CreationException {
        DeleteServiceInstanceRequest updateServiceInstanceRequest =
                DeleteServiceInstanceRequest
                        .builder()
                        .name(serviceInstanceName)
                        .build();

        try {
            //unbind route
            ListRoutesRequest requ = ListRoutesRequest.builder().build();
            List<Route> routes = this.cloudFoundryOperations.routes().list(requ).collectList().block();


            for (Route route : routes) {
                UnbindRouteServiceInstanceRequest r = UnbindRouteServiceInstanceRequest.builder()
                        .serviceInstanceName(serviceInstanceName)
                        .domainName(route.getDomain())
                        .build();


                this.cloudFoundryOperations.services().unbindRoute(r).block();

            }


            //unbound apps
            GetServiceInstanceRequest getServiceInstanceRequest = GetServiceInstanceRequest
                    .builder()
                    .name(serviceInstanceName)
                    .build();

            ServiceInstance serviceInstance = this.cloudFoundryOperations
                    .services()
                    .getInstance(getServiceInstanceRequest)
                    .block();

            if (serviceInstance != null) {
                for (String app : serviceInstance.getApplications()) {
                    UnbindServiceInstanceRequest request = UnbindServiceInstanceRequest.builder()
                            .serviceInstanceName(serviceInstanceName)
                            .applicationName(app)
                            .build();

                    this.cloudFoundryOperations.services().unbind(request).block();
                }
            }

            // delete keys
            ListServiceKeysRequest requestKEy = ListServiceKeysRequest.builder().serviceInstanceName(serviceInstanceName).build();

            List<ServiceKey> keys = this.cloudFoundryOperations.services().listServiceKeys(requestKEy).collectList().block();

            for (ServiceKey key : keys) {
                DeleteServiceKeyRequest requests = DeleteServiceKeyRequest.builder()
                        .serviceInstanceName(serviceInstanceName)
                        .serviceKeyName(key.getName())
                        .build();

                this.cloudFoundryOperations.services().deleteServiceKey(requests).block();

            }

            this.cloudFoundryOperations.services()
                    .deleteInstance(updateServiceInstanceRequest)
                    .block();
            Log.info("Service " + serviceInstanceName + " has been removed.");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }
    }
}
