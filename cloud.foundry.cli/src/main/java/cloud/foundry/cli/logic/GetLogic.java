package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.logging.Log;
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
     * Gets all the necessary configuration-information from a cloud foundry
     * instance.
     *
     * @return ConfigBean
     */
    public ConfigBean getAll() {
        Mono<String> apiVersion = determineApiVersion();
        Mono<List<String>> spaceDevelopers = getSpaceDevelopers();
        Mono<Map<String, ServiceBean>> services = getServices();
        Mono<Map<String, ApplicationBean>> apps = getApplications();
        ConfigBean configBean = new ConfigBean();
        SpecBean specBean = new SpecBean();
        // start async querying of config data from the cloud foundry instance
        Log.debug("Start async querying of apps, services and space developers...");
        Flux.merge(apiVersion.doOnSuccess(configBean::setApiVersion),
                spaceDevelopers.doOnSuccess(specBean::setSpaceDevelopers),
                services.doOnSuccess(specBean::setServices),
                apps.doOnSuccess(specBean::setApps))
                .blockLast();

        configBean.setSpec(specBean);
        configBean.setTarget(determineTarget());
        return configBean;
    }

    /**
     * Gets all the necessary space-developer-information from a cloud foundry instance.
     *
     * @return List of space-developers.
     */
    public Mono<List<String>> getSpaceDevelopers() {
        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cloudFoundryOperations);
        return spaceDevelopersOperations.getAll();
    }

    /**
     * Gets all the necessary service-information from a cloud foundry instance.
     *
     * @return Map of services.
     */
    public Mono<Map<String, ServiceBean>> getServices() {
        ServicesOperations servicesOperations = new ServicesOperations(cloudFoundryOperations);
        return servicesOperations.getAll();
    }

    /**
     * Gets all the necessary application-information from a cloud foundry instance.
     *
     * @return Map of applications.
     */
    public Mono<Map<String, ApplicationBean>> getApplications() {
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cloudFoundryOperations);
        return applicationsOperations.getAll();
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
     * Determines the Target-Node configuration-information from a cloud foundry
     * instance.
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