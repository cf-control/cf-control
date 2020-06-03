package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;

import java.util.List;
import java.util.Map;

import cloud.foundry.cli.operations.AbstractOperations;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
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
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cloudFoundryOperations);

        Mono<String> apiVersion = determineApiVersion();
        Mono<List<String>> spaceDevelopers = spaceDevelopersOperations.getAll();
        Mono<Map<String, ServiceBean>> services = servicesOperations.getAll();
        Mono<Map<String, ApplicationBean>> apps = applicationsOperations.getAll();

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
     * Determines the Spec-Node configuration-information from a cloud foundry instance.
     *
     * @return SpecBean
     */
    private SpecBean determineSpec() {
        SpecBean spec = new SpecBean();

        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        Mono<List<String>> spaceDevelopers = spaceDevelopersOperations.getAll();
        spec.setSpaceDevelopers(spaceDevelopers.block());

        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        Mono<Map<String, ServiceBean>> services = servicesOperations.getAll();
        spec.setServices(services.block());

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cloudFoundryOperations);
        Mono<Map<String, ApplicationBean>> applications = applicationsOperations.getAll();
        spec.setApps(applications.block());

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
