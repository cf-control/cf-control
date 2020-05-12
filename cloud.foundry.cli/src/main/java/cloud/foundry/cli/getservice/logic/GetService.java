package cloud.foundry.cli.getservice.logic;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;

public class GetService {

    private CloudFoundryOperations cfOperations;

    public GetService(CloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }

    public String getServices() {
        List<ServiceInstanceSummary> services = this.cfOperations.services()
                .listInstances().collectList().block();
        for (ServiceInstanceSummary serviceInstanceSummary : services) {
            // do not dump tags into the document
            ServiceInstanceSummaryBean serviceInstance = new ServiceInstanceSummaryBean(serviceInstanceSummary);
            DumperOptions options = new DumperOptions();
            options.setTags(new HashMap<String, String>()); // do not dump tags into the document
            Yaml yaml = new Yaml(options);
            String yamlDocument = yaml.dumpAsMap(serviceInstance);
            return yamlDocument;
        }
        return null;
    }

    public CloudFoundryOperations getCfOperations() {
        return cfOperations;
    }

    public void setCfOperations(CloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }
}