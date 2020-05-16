package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ServiceInstanceSummaryBean;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;

public class ServicesOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public ServicesOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }


    public List<ServiceInstanceSummaryBean> getAll() {
        List<ServiceInstanceSummary> services = this.cloudFoundryOperations
                .services()
                .listInstances()
                .collectList()
                .block();


        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ServiceInstanceSummaryBean> beans = new ArrayList<>();
        for (ServiceInstanceSummary serviceInstanceSummary : services) {
            beans.add(new ServiceInstanceSummaryBean(serviceInstanceSummary));
        }
        // create YAML document
        Yaml yaml = YamlCreator.createDefaultYamlProcessor();

        return yaml.loadAs(yaml.dump(beans), List.class);
    }

}
