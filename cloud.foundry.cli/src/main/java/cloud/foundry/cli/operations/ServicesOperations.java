package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.yaml.snakeyaml.Yaml;
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
     * @throws CreationException when the creation was not successful
     */
    public void create(ServiceBean serviceBean) throws CreationException {
        CreateServiceInstanceRequest.Builder createServiceBuilder = CreateServiceInstanceRequest.builder();
        createServiceBuilder.serviceName(serviceBean.getService());
        createServiceBuilder.planName(serviceBean.getPlan());
        createServiceBuilder.serviceInstanceName(serviceBean.getName());
        Mono<Void> created = this.cloudFoundryOperations.services().createInstance(createServiceBuilder.build());
        try {
            created.block();
            System.out.println("Service has been created.");
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        //Bind apps to service
        List<String> applications = serviceBean.getApplications();
        for (String app: applications) {
            BindServiceInstanceRequest.Builder bindServiceBuilder = BindServiceInstanceRequest.builder();
            bindServiceBuilder.applicationName(app);
            bindServiceBuilder.serviceInstanceName(serviceBean.getName());
            Mono<Void> bind = this.cloudFoundryOperations.services().bind(bindServiceBuilder.build());
            try {
                bind.block();
                System.out.println("Service has been bound.");
            } catch (Exception e) {
                throw new CreationException(e.getMessage());
            }
        }
    }

}
