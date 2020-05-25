package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.operations.services.ServiceInstance;
import cloud.foundry.cli.crosscutting.logging.Log;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.RenameServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.UpdateServiceInstanceRequest;

import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    public List<ServiceBean> getAll() {
        List<ServiceInstanceSummary> services = this.cloudFoundryOperations
            .services()
            .listInstances()
            .collectList()
            .block();

        if (services == null) {
            services = new LinkedList<>();
        }

        // create a list of special bean data objects, as the summaries cannot be
        // serialized directly
        List<ServiceBean> beans = new ArrayList<>(services.size());
        for (ServiceInstanceSummary serviceInstanceSummary : services) {

            GetServiceInstanceRequest getServiceInstanceRequest = GetServiceInstanceRequest.builder()
                .name(serviceInstanceSummary.getName())
                .build();
            ServiceInstance serviceInstance = this.cloudFoundryOperations
                .services()
                .getInstance(getServiceInstanceRequest)
                .block();

            ServiceBean serviceBean = new ServiceBean(serviceInstanceSummary);

            if (serviceInstance.getLastOperation() == null
                || serviceInstance.getLastOperation().isEmpty()) {
             
                serviceBean.setLastOperation("");
            } else {
                serviceBean
                    .setLastOperation(serviceInstance.getLastOperation() + " " + serviceInstance.getStatus());
            }
            beans.add(serviceBean);
        }
        return beans;
    }

    /**
     * Creates a new service in the space and binds apps to it. In case of an error,
     * the creation- and binding-process is discontinued.
     * 
     * @param serviceBean serves as template for the service to create
     * @throws CreationException when the creation or the binding was not successful
     */
    public void create(ServiceBean serviceBean) throws CreationException {
        CreateServiceInstanceRequest createServiceRequest = CreateServiceInstanceRequest.builder()
            .serviceName(serviceBean.getService())
            .planName(serviceBean.getPlan())
            .serviceInstanceName(serviceBean.getName())
            .tags(serviceBean.getTags())
            .build();

        Mono<Void> created = this.cloudFoundryOperations.services().createInstance(createServiceRequest);

        try {
            created.block();
            Log.info("Service \"" + serviceBean.getName() + "\" has been created.");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }

        // bind the newly created service instance to its applications
        bindToApplications(serviceBean);
    }

    /**
     * Update a service instance
     * @param serviceBean serves as template for the service to update
     * @throws CreationException when the creation or the binding was not successful
     */
    public void update(ServiceBean serviceBean) throws CreationException {
        System.out.println("Updating service instance ID " + serviceBean.getId());

        // rename a service instance
        // TODO currentname should be changed, because we have no idea
        // about the format of the input yaml file
        String currentname = "Elephant3";
        renameService(serviceBean.getName(), currentname);

        // update plan, tags of a service instance
        updateServiceInstance(serviceBean);

        // bind a service instance to applications
        bindToApplications(serviceBean);

    }

    /**
     * Rename a service instance
     * @param currentName Current Name of the Service Instance
     * @param newName     New Name of the Service Instance
     */
    private void renameService(String newName, String currentName) throws CreationException {
        RenameServiceInstanceRequest renameServiceInstanceRequest = RenameServiceInstanceRequest.builder()
            .name(currentName)
            .newName(newName)
            .build();

        try {
            this.cloudFoundryOperations.services()
                .renameInstance(renameServiceInstanceRequest)
                .block();
            System.out.println("Name of Service Instance has been changed");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }
    }

    /**
     * Update Tags, Plan of a Service Instance
     * @param serviceBean serves as template for the service to update
     */
    private void updateServiceInstance(ServiceBean serviceBean) throws CreationException {
        UpdateServiceInstanceRequest updateServiceInstanceRequest = UpdateServiceInstanceRequest.builder()
            .serviceInstanceName(serviceBean.getName())
            .tags(serviceBean.getTags())
            .planName(serviceBean.getPlan())
            .build();

        try {
            this.cloudFoundryOperations.services()
                .updateInstance(updateServiceInstanceRequest)
                .block();
            System.out.println("Service Plan and Tags haven been updated");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }
    }

    /**
     * Bind a service instance to applications
     * @param ServiceBean serves as template for the service to update
     */
    private void bindToApplications(ServiceBean serviceBean) throws CreationException {
        String service = serviceBean.getName();

        for (String app : serviceBean.getApplications()) {
            BindServiceInstanceRequest bindServiceRequest = BindServiceInstanceRequest.builder()
                .applicationName(app)
                .serviceInstanceName(service)
                .build();

            try {
                this.cloudFoundryOperations.services()
                    .bind(bindServiceRequest)
                    .block();
                Log.info("Service \"" + service + "\" has been bound to the application \"" + app + "\".");
            } catch (RuntimeException e) {
                throw new CreationException(e.getMessage());
            }
        }
    }
}
