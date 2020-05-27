package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
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

        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ServiceBean> beans = new ArrayList<>(services.size());
        for (ServiceInstanceSummary serviceInstanceSummary : services) {
            beans.add(new ServiceBean(serviceInstanceSummary));
        }

        return beans;
    }

    /**
     * Creates a new service in the space and binds apps to it. In case of an error, the creation- and binding-process
     * is discontinued.
     * @param serviceBean serves as template for the service to create
     * @throws CreationException when the creation or the binding was not successful
     */
    public void create(ServiceBean serviceBean) throws CreationException {
        CreateServiceInstanceRequest createServiceRequest = CreateServiceInstanceRequest.builder()
                .serviceName(serviceBean.getService())
                .planName(serviceBean.getPlan())
                .serviceInstanceName(serviceBean.getName())
                .build();

        Mono<Void> created = this.cloudFoundryOperations.services().createInstance(createServiceRequest);

        try {
            created.block();
            System.out.println("Service \"" + serviceBean.getName() + "\" has been created.");
        } catch (RuntimeException e) {
            throw new CreationException(e.getMessage());
        }

        // bind the newly created service instance to its applications
        List<String> applications = serviceBean.getApplications();
        for (String app: applications) {
            BindServiceInstanceRequest bindServiceRequest = BindServiceInstanceRequest.builder()
                .applicationName(app)
                .serviceInstanceName(serviceBean.getName())
                .build();

            Mono<Void> bind = this.cloudFoundryOperations.services().bind(bindServiceRequest);

            try {
                bind.block();
                System.out.println("Service has been bound to the application \"" + app + "\".");
            } catch (RuntimeException e) {
                throw new CreationException(e.getMessage());
            }
        }
    }

}
