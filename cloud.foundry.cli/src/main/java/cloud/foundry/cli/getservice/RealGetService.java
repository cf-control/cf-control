package cloud.foundry.cli.getservice;

import cloud.foundry.cli.getservice.logic.ServiceInstanceSummaryBean;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;

public class RealGetService {


    public String toTest(DefaultCloudFoundryOperations cfOperations) {
        List<ServiceInstanceSummary> services = cfOperations.services()
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
        return "";
    }
}