package cloud.foundry.cli.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.TargetBean;
import cloud.foundry.cli.crosscutting.beans.SpecBean;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * Handles the operations to receive all configuration-information from a cloud
 * foundry instance.
 */
public class AllInformationOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public AllInformationOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * Gets all the necessary configuration-information from a cloud foundry instance.
     *
     * @return ConfigBean
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
     * Determines the API-Version from a cloud foundry instance.
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
