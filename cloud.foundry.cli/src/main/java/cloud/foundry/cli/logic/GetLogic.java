package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;

import java.util.List;
import java.util.Map;

import cloud.foundry.cli.operations.AbstractOperations;
import cloud.foundry.cli.operations.ApplicationOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;


/**
 * Handles the operations to receive all configuration-information from a cloud
 * foundry instance.
 */
public class GetLogic extends AbstractOperations<DefaultCloudFoundryOperations> {

    public GetLogic(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * Gets all the necessary configuration-information from a cloud foundry instance.
     *
     * @return ConfigBean
     */
    public ConfigBean getAll() {
        ConfigBean allInformation = new ConfigBean();
        allInformation.setApiVersion(determineApiVersion());
        allInformation.setTarget(determineTarget());
        allInformation.setSpec(determineSpec());

        return allInformation;
    }

    /**
     * Determines the API-Version from a cloud foundry instance.
     *
     * @return API-Version
     */
    private String determineApiVersion() {
        CloudFoundryClient cfClient = cloudFoundryOperations.getCloudFoundryClient();
        GetInfoRequest infoRequest = GetInfoRequest.builder().build();
        Info cfClientInfo = cfClient.info();

        return cfClientInfo.get(infoRequest).block().getApiVersion();
    }

    /**
     * Determines the Spec-Node configuration-information from a cloud foundry instance.
     *
     * @return SpecBean
     */
    private SpecBean determineSpec() {
        SpecBean spec = new SpecBean();

        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        List<String> spaceDevelopers = spaceDevelopersOperations.getAll();
        spec.setSpaceDevelopers(spaceDevelopers);

        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        Map<String, ServiceBean> services = servicesOperations.getAll();
        spec.setServices(services);

        ApplicationOperations applicationOperations = new ApplicationOperations(cloudFoundryOperations);
        Map<String, ApplicationBean> applications = applicationOperations.getAll();
        spec.setApps(applications);

        return spec;
    }


    /**
     * Determines the Target-Node configuration-information from a cloud foundry instance.
     *
     * @return TargetBean
     */
    private TargetBean determineTarget() {
        ReactorCloudFoundryClient rcl = (ReactorCloudFoundryClient) cloudFoundryOperations.getCloudFoundryClient();
        DefaultConnectionContext cc = (DefaultConnectionContext) rcl.getConnectionContext();

        TargetBean target = new TargetBean();
        target.setEndpoint(cc.getApiHost());
        target.setOrg(cloudFoundryOperations.getOrganization());
        target.setSpace(cloudFoundryOperations.getSpace());

        return target;
    }
}
