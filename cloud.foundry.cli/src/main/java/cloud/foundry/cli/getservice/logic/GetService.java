package cloud.foundry.cli.getservice.logic;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GetService {

    private CloudFoundryOperations cfOperations;

    public GetService(CloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }

    /**
     * Factory function creating a Yaml object with common options. Ensures a consistent output format.
     * @return Yaml object preconfigured with proper options
     */
    private static Yaml makeYaml() {
        DumperOptions options = new DumperOptions();

        // do not dump tags into the document
        options.setTags(new HashMap<>());

        // format all nested mappings in block style
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        // indentation aids readability
        options.setIndent(2);

        // use custom representer to hide bean class names in output
        // we explicitly have to add _all_ custom bean types
        Representer representer = new Representer();
        representer.addClassTag(ServiceInstanceSummaryBean.class, Tag.MAP);
        representer.addClassTag(ApplicationBean.class, Tag.MAP);
        representer.addClassTag(ApplicationManifestBean.class, Tag.MAP);

        return new Yaml(representer, options);
    }

    public String getServices() {
        List<ServiceInstanceSummary> services = this.cfOperations.services().listInstances().collectList().block();

        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ServiceInstanceSummaryBean> beans = new ArrayList<>();
        for (ServiceInstanceSummary serviceInstanceSummary : services) {
            beans.add(new ServiceInstanceSummaryBean(serviceInstanceSummary));
        }

        // create YAML document
        Yaml yaml = makeYaml();
        String yamlDocument = yaml.dump(beans);

        return yamlDocument;
    }

    public String getApplications() {
        List<ApplicationSummary> applications = this.cfOperations.applications().list().collectList().block();

        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ApplicationBean> beans = new ArrayList<>();
        for (ApplicationSummary summary : applications) {
            GetApplicationManifestRequest manifestRequest = GetApplicationManifestRequest.builder()
                    .name(summary.getName()).build();
            ApplicationManifest manifest = this.cfOperations.applications()
                    .getApplicationManifest(manifestRequest).block();

            beans.add(new ApplicationBean(manifest));
        }

        // create YAML document
        Yaml yaml = makeYaml();
        String yamlDocument = yaml.dump(beans);

        return yamlDocument;
    }

    public CloudFoundryOperations getCfOperations() {
        return cfOperations;
    }

    public void setCfOperations(CloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }
}