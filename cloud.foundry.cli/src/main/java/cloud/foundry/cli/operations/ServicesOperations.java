package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.operations.services.ServiceInstance;
import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;

import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.LinkedList;
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
     * @return all service instances in the space
     */
    public Map<String, ServiceBean> getAll() {
        List<ServiceInstanceSummary> services = this.cloudFoundryOperations
            .services()
            .listInstances()
            .collectList()
            .block();

        if (services == null) {
            services = new LinkedList<>();
        }

        // create a map of special bean data objects, as the summaries cannot be serialized directly
        Map<String, ServiceBean> mapBeans = new HashMap<>();

        for (ServiceInstanceSummary serviceInstanceSummary : services) {

            GetServiceInstanceRequest getServiceInstanceRequest = GetServiceInstanceRequest.builder()
                .name(serviceInstanceSummary.getName())
                .build();
            ServiceInstance serviceInstance = this.cloudFoundryOperations
                .services()
                .getInstance(getServiceInstanceRequest)
                .block();

            ServiceBean serviceBean = new ServiceBean(serviceInstance);
            mapBeans.put(serviceInstance.getName(), serviceBean);
        }

        return mapBeans;

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
}
