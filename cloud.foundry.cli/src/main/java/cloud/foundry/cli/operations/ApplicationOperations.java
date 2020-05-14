package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.Bean;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;

import java.util.ArrayList;
import java.util.List;

public class ApplicationOperations extends AbstractOperations<DefaultCloudFoundryOperations>{

    public ApplicationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    @Override
    public void create(Bean bean) {

    }

    @Override
    public void delete(Bean bean) {

    }

    @Override
    public void update(Bean bean) {

    }

    @Override
    public List<ApplicationBean> get() {
        List<ApplicationSummary> applications = this.cloudFoundryOperations.applications().list().collectList().block();

        // create a list of special bean data objects, as the summaries cannot be serialized directly
        List<ApplicationBean> beans = new ArrayList<>();
        for (ApplicationSummary summary : applications) {
            GetApplicationManifestRequest manifestRequest = GetApplicationManifestRequest.builder()
                    .name(summary.getName()).build();
            ApplicationManifest manifest = this.cloudFoundryOperations.applications()
                    .getApplicationManifest(manifestRequest).block();

            beans.add(new ApplicationBean(manifest));
        }

     /*   // create YAML document
        Yaml yaml = makeYaml();
        String yamlDocument = yaml.dump(beans);*/

        return null;
    }
}
