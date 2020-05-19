package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ServiceInstanceSummaryBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;

import java.util.*;

/**
 * Handles the operations to receive all configuration-information from a cloud foundry instance.
 */
public class AllInformationOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public static final String SPACE_DEVELOPERS = "spaceDevelopers";
    public static final String SERVICES = "services";
    public static final String APPLICATIONS = "applications";

    public static final String API_ENDPOINT = "api endpoint";
    public static final String ORG = "org";
    public static final String SPACE = "space";

    public AllInformationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    public GetAllBean getAll() {
        GetAllBean allInformation = new GetAllBean();
        allInformation.setApiVersion(determineAPIVersion());
        allInformation.setTarget(determineTarget());
        allInformation.setSpec(determineSpec());

        return allInformation;
    }

    private String determineAPIVersion() {
        CloudFoundryClient cfClient = cloudFoundryOperations.getCloudFoundryClient();
        GetInfoRequest infoRequest = GetInfoRequest.builder().build();
        Info cfClientInfo = cfClient.info();

        return cfClientInfo.get(infoRequest).block().getApiVersion();
    }

    private Map<String, Object> determineSpec() {
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        SpaceDevelopersBean spaceDevelopers = spaceDevelopersOperations.getAll();

        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        List<ServiceInstanceSummaryBean> services = servicesOperations.getAll();

        ApplicationOperations applicationOperations = new ApplicationOperations(cloudFoundryOperations);
        List<ApplicationBean> applications = applicationOperations.getAll();

        Map<String, Object> spec = new HashMap<>();
        spec.put(SPACE_DEVELOPERS, spaceDevelopers.getSpaceDevelopers());
        spec.put(SERVICES, services);
        spec.put(APPLICATIONS, applications);

        return spec;
    }

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
