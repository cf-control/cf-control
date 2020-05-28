package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class AllInformationOperations extends AbstractOperations<DefaultCloudFoundryOperations> {
    private static final String SPACE_DEVELOPERS = "spaceDevelopers";
    private static final String SERVICES = "services";
    private static final String APPLICATIONS = "applications";

    private static final String API_ENDPOINT = "api endpoint";
    private static final String ORG = "org";
    private static final String SPACE = "space";

    public AllInformationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * Gets all the necessary configuration-information from a cloud foundry instance.
     *
     * @return GetAllBean
     */
    public GetAllBean getAll() {
        GetAllBean allInformation = new GetAllBean();
        allInformation.setApiVersion(determineApiVersion());
        allInformation.setTarget(determineTarget());
        allInformation.setSpec(determineSpec());

        return allInformation;
    }

    /**
     * Determines the API-Version from a a cloud foundry instance.
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
     * Determines the Spec-Node configuration-information from a a cloud foundry instance.
     *
     * @return Spec-Data
     */
    private Map<String, Object> determineSpec() {
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        SpaceDevelopersBean spaceDevelopers = spaceDevelopersOperations.getAll();

        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        List<ServiceBean> services = servicesOperations.getAll();

        ApplicationOperations applicationOperations = new ApplicationOperations(cloudFoundryOperations);
        List<ApplicationBean> applications = applicationOperations.getAll();

        Map<String, Object> spec = new HashMap<>();
        spec.put(SPACE_DEVELOPERS, spaceDevelopers.getSpaceDevelopers());
        spec.put(SERVICES, services);
        spec.put(APPLICATIONS, applications);

        return spec;
    }


    /**
     * Determines the Target-Node configuration-information from a a cloud foundry instance.
     *
     * @return Target-Data
     */
    private Map<String, String> determineTarget() {
        ReactorCloudFoundryClient rcl = (ReactorCloudFoundryClient) cloudFoundryOperations.getCloudFoundryClient();
        DefaultConnectionContext cc = (DefaultConnectionContext) rcl.getConnectionContext();

        Map<String, String> target = new HashMap<>();
        target.put(API_ENDPOINT, cc.getApiHost());
        target.put(ORG, cloudFoundryOperations.getOrganization());
        target.put(SPACE, cloudFoundryOperations.getSpace());

        return target;
    }
}
