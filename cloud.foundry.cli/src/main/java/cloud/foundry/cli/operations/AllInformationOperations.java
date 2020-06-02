package cloud.foundry.cli.operations;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public ConfigBean getAll() {
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        ApplicationOperations applicationOperations = new ApplicationOperations(cloudFoundryOperations);

        Mono<String> apiVersion = determineApiVersion();
        Mono<List<String>> spaceDevelopers = spaceDevelopersOperations.getAll();
        Mono<Map<String, ServiceBean>> services = servicesOperations.getAll();
        Mono<Map<String, ApplicationBean>> apps = applicationOperations.getAll();

        ConfigBean configBean = new ConfigBean();
        SpecBean specBean = new SpecBean();

        // start async querying of config data from the cloud foundry instance
        Flux.merge(apiVersion.doOnSuccess(configBean::setApiVersion),
                spaceDevelopers.doOnSuccess(s -> specBean.setSpaceDevelopers(s)),
                services.doOnSuccess(specBean::setServices),
                apps.doOnSuccess(specBean::setApps))
            .blockLast();

        configBean.setSpec(specBean);
        configBean.setTarget(determineTarget());

        return configBean;
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
