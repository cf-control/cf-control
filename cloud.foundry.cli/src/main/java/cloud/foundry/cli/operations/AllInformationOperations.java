package cloud.foundry.cli.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        ApplicationOperations applicationOperations = new ApplicationOperations(cloudFoundryOperations);

        Mono<String> apiVersion = determineApiVersion();
        Mono<SpaceDevelopersBean> spaceDevelopers = spaceDevelopersOperations.getAll();
        Mono<List<ServiceBean>> services = servicesOperations.getAll();
        Mono<List<ApplicationBean>> apps = applicationOperations.getAll();

        GetAllBean allInformation = new GetAllBean();
        Map<String, Object> spec = new HashMap<>();

        // start async querying of config data from the cloud foundry instance
        Flux.merge(apiVersion.doOnSuccess(allInformation::setApiVersion),
                spaceDevelopers.doOnSuccess(s -> spec.put(SPACE_DEVELOPERS, s.getSpaceDevelopers())),
                services.doOnSuccess(s -> spec.put(SERVICES, s)),
                apps.doOnSuccess(a ->  spec.put(APPLICATIONS, a)))
            .blockLast();

        allInformation.setSpec(spec);
        allInformation.setTarget(determineTarget());

        return allInformation;
    }

    /**
     * Determines the API-Version from a a cloud foundry instance.
     *
     * @return API-Version
     */
    private Mono<String> determineApiVersion() {
        CloudFoundryClient cfClient = cloudFoundryOperations.getCloudFoundryClient();
        GetInfoRequest infoRequest = GetInfoRequest.builder().build();
        Info cfClientInfo = cfClient.info();

        return cfClientInfo.get(infoRequest).map(GetInfoResponse::getApiVersion);
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