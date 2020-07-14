package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;

import java.util.List;
import java.util.Map;

import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import cloud.foundry.cli.operations.ClientOperations;
import cloud.foundry.cli.services.LoginCommandOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Handles the operations to receive all configuration-information from a cloud
 * foundry instance.
 */
public class GetLogic {

    private static final Log log = Log.getLog(GetLogic.class);


    /**
     * Gets all the necessary configuration-information from a cloud foundry
     * instance.
     *
     * @param spaceDevelopersOperations SpaceDevelopersOperations
     * @param servicesOperations ServicesOperations
     * @param applicationsOperations ApplicationsOperations
     * @param clientOperations ClientOperations
     * @param loginOptions LoginCommandOptions
     * @return ConfigBean
     * @throws GetException if an error occurs during the information retrieving
     */
    public ConfigBean getAll(SpaceDevelopersOperations spaceDevelopersOperations,
                             ServicesOperations servicesOperations,
                             ApplicationsOperations applicationsOperations,
                             ClientOperations clientOperations,
                             LoginCommandOptions loginOptions) {

        Mono<String> apiVersion = clientOperations.determineApiVersion();
        Mono<List<String>> spaceDevelopers = spaceDevelopersOperations.getAll();
        Mono<Map<String, ServiceBean>> services = servicesOperations.getAll();
        Mono<Map<String, ApplicationBean>> apps = applicationsOperations.getAll();
        ConfigBean configBean = new ConfigBean();
        SpecBean specBean = new SpecBean();
        // start async querying of config data from the cloud foundry instance
        log.debug("Start async querying of apps, services and space developers...");
        Flux<Object> getAllRequests = Flux.merge(apiVersion.doOnSuccess(configBean::setApiVersion),
                spaceDevelopers.doOnSuccess(specBean::setSpaceDevelopers),
                services.doOnSuccess(specBean::setServices),
                apps.doOnSuccess(specBean::setApps));

        try {
            getAllRequests.blockLast();
        } catch (RuntimeException e) {
            throw new GetException(e);
        }

        configBean.setSpec(specBean);
        configBean.setTarget(determineTarget(loginOptions));
        return configBean;
    }

    /**
     * Gets all the necessary space-developer-information from a cloud foundry instance.
     *
     * @return List of space-developers.
     * @throws GetException if an error occurs during the information retrieving
     */
    List<String> getSpaceDevelopers(SpaceDevelopersOperations spaceDevelopersOperations) {
        Mono<List<String>> getSpaceDevelopersRequest = spaceDevelopersOperations.getAll();

        try {
            return getSpaceDevelopersRequest.block();
        } catch (RuntimeException e) {
            throw new GetException(e);
        }
    }

    /**
     * Gets all the necessary service-information from a cloud foundry instance.
     *
     * @return Map of services.
     * @throws GetException if an error occurs during the information retrieving
     */
    Map<String, ServiceBean> getServices(ServicesOperations servicesOperations) {
        Mono<Map<String, ServiceBean>> getServicesRequest = servicesOperations.getAll();

        try {
            return getServicesRequest.block();
        } catch (RuntimeException e) {
            throw new GetException(e);
        }
    }

    /**
     * Gets all the necessary application-information from a cloud foundry instance.
     *
     * @return Map of applications.
     * @throws GetException if an error occurs during the information retrieving
     */
    Map<String, ApplicationBean> getApplications(ApplicationsOperations applicationsOperations) {
        Mono<Map<String, ApplicationBean>> getApplicationsRequest = applicationsOperations.getAll();

        try {
            return getApplicationsRequest.block();
        } catch (RuntimeException e) {
            throw new GetException(e);
        }
    }

    /**
     * Determines the Target-Node configuration-information from a cloud foundry
     * instance.
     *
     * @param loginOptions LoginCommandOptions
     * @return TargetBean
     */
    private TargetBean determineTarget(LoginCommandOptions loginOptions) {
        TargetBean target = new TargetBean();
        target.setEndpoint(loginOptions.getApiHost());
        target.setOrg(loginOptions.getOrganization());
        target.setSpace(loginOptions.getSpace());
        return target;
    }

}