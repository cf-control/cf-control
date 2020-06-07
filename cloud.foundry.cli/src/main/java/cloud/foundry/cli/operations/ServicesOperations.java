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

    public void removeServiceInstance(String serviceInstanceName) throws CreationException {

        try {
            // unbind route
            System.out.print(" ROUTE");
            ListRoutesRequest requ = ListRoutesRequest.builder().build();
            List<Route> routes = this.cloudFoundryOperations.routes().list(requ).collectList().block();

            for (Route route : routes) {
                if (route.getService() != null && route.getService().equals(serviceInstanceName)) {
                    UnbindRouteServiceInstanceRequest r = UnbindRouteServiceInstanceRequest.builder()
                        .serviceInstanceName(serviceInstanceName)
                        .domainName(route.getDomain())
                        .hostname(route.getHost())
                        .build();

                    try {
                        this.cloudFoundryOperations.services().unbindRoute(r).block();

                        System.out.print("UNBIND ROUTE");

                    } catch (Exception e) {
                        System.err.print("route" + e);
                    }
                }
            }

            // unbound apps
            System.out.print(" APPS");
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

                    try {
                        this.cloudFoundryOperations.services().unbind(request).block();
                        System.out.print("UNBIND apps");

                    } catch (Exception e) {
                        System.err.print("apps" + e);
                    }
                }
            }

            // delete keys
            System.out.print("KEYS");
                        
            if (!serviceInstance.getType().getValue().equals("user_provided_service_instance")) {
                
                ListServiceKeysRequest requestKEy = ListServiceKeysRequest.builder()
                    .serviceInstanceName(serviceInstanceName)
                    .build();

                List<ServiceKey> keys = this.cloudFoundryOperations.services().listServiceKeys(requestKEy)
                    .collectList()
                    .block();

                for (ServiceKey key : keys) {
                    DeleteServiceKeyRequest requests = DeleteServiceKeyRequest.builder()
                        .serviceInstanceName(serviceInstanceName)
                        .serviceKeyName(key.getName())
                        .build();

                    try {
                        this.cloudFoundryOperations.services().deleteServiceKey(requests).block();
                        System.out.print("delete key");

                    } catch (Exception e) {
                        System.err.print("key" + e);
                    }
                }
            }

            
            DeleteServiceInstanceRequest updateServiceInstanceRequest = DeleteServiceInstanceRequest
                .builder()
                .name(serviceInstanceName)
                .build();

            this.cloudFoundryOperations.services()
                .deleteInstance(updateServiceInstanceRequest)
                .block();
            Log.info("Service " + serviceInstanceName + " has been removed.");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }
    }
}
