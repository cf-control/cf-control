package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.logic.apply.ApplicationRequestsPlaner;
import cloud.foundry.cli.logic.apply.ServiceRequestsPlaner;
import cloud.foundry.cli.logic.apply.SpaceDevelopersRequestsPlaner;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import cloud.foundry.cli.operations.SpaceOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * This class takes care of applying desired cloud foundry configurations to a
 * live system.
 */
public class ApplyLogic {

    private static final Log log = Log.getLog(ApplyLogic.class);

    private DefaultCloudFoundryOperations cfOperations;

    private GetLogic getLogic;

    private DiffLogic diffLogic;

    private SpaceDevelopersOperations spaceDevelopersOperations;
    private ServicesOperations servicesOperations;
    private ApplicationsOperations applicationsOperations;
    private SpaceOperations spaceOperations;


    /**
     * Creates a new instance that will use the provided cf operations internally.
     * @param cfOperations the cf operations that should be used to communicate with
     *                     the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
        this.spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        this.servicesOperations = new ServicesOperations(cfOperations);
        this.applicationsOperations = new ApplicationsOperations(cfOperations);
        this.spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        this.getLogic = new GetLogic();
        this.diffLogic = new DiffLogic();
    }

    public void setApplicationsOperations(ApplicationsOperations applicationsOperations) {
        this.applicationsOperations = applicationsOperations;
    }

    public void setSpaceOperations(SpaceOperations spaceOperations) {
        this.spaceOperations = spaceOperations;
    }

    /**
     * Assign users as space developers that are not present in the live system and
     * revoke space developers permission, if its present in the live system but not
     * defined in <code>desiredSpaceDevelopers</code>. In case of any non-recoverable error, the
     * procedure is discontinued.
     *
     * @param desiredSpaceDevelopers the space developers that should all be present
     *                               in the live system after the procedure.
     * @throws NullPointerException if the argument is null.
     */
    public void applySpaceDevelopers(@Nonnull List<String> desiredSpaceDevelopers) {
        checkNotNull(desiredSpaceDevelopers);

        log.info("Fetching space developers data");
        List<String> liveSpaceDevelopers = this.getLogic.getSpaceDevelopers(spaceDevelopersOperations);
        log.verbose("Fetching space developers data completed");

        ConfigBean desiredSpaceDevelopersConfig = createConfigFromSpaceDevelopers(desiredSpaceDevelopers);
        ConfigBean liveSpaceDevelopersConfig = createConfigFromSpaceDevelopers(liveSpaceDevelopers);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        log.info("Diffing space developers");
        DiffResult wrappedDiff = this.diffLogic.createDiffResult(liveSpaceDevelopersConfig,
                desiredSpaceDevelopersConfig);
        log.verbose("Diffing space developers completed");

        CfContainerChange spaceDevelopersChange = wrappedDiff.getSpaceDevelopersChange();
        if (spaceDevelopersChange == null) {
            log.info("No changes detected, nothing to apply");
        } else {
            Flux<Void> spaceDevelopersRequests = Flux.just(spaceDevelopersChange)
                    .flatMap(spaceDeveloperChange -> SpaceDevelopersRequestsPlaner
                            .createSpaceDevelopersRequests(spaceDevelopersOperations, spaceDeveloperChange))
                    .onErrorContinue(log::warning);
            
            log.info("Applying changes to space developers");
            spaceDevelopersRequests.blockLast();
            log.verbose("Changes to space developers applied");
        }
    }

    /**
     * Apply the differences between the applications given in the yaml file and the configuration
     * of the applications of your cf instance. In case of any non-recoverable error,
     * the procedure is discontinued.
     *
     * @param desiredApplications the applications that should all be present in the
     *                            live system after the procedure
     * @throws ApplyException       if an non-recoverable error occurs during the procedure
     * @throws NullPointerException if the argument is null
     */
    public void applyApplications(@Nonnull Map<String, ApplicationBean> desiredApplications) {
        checkNotNull(desiredApplications);

        log.info("Fetching applications data");
        Map<String, ApplicationBean> liveApplications = this.getLogic.getApplications(applicationsOperations);
        log.verbose("Fetched space developers data");

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredApplicationsConfig = createConfigFromApplications(desiredApplications);
        ConfigBean liveApplicationsConfig = createConfigFromApplications(liveApplications);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        log.info("Diffing applications");
        DiffResult diffResult = this.diffLogic.createDiffResult(liveApplicationsConfig, desiredApplicationsConfig);
        log.verbose("Diffing applications completed");

        Map<String, List<CfChange>> allApplicationChanges = diffResult.getApplicationChanges();

        if (allApplicationChanges == null || allApplicationChanges.isEmpty()) {
            log.info("No changes detected, nothing to apply");
        } else {
            ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(applicationsOperations);

            Flux<Void> applicationRequests = Flux.fromIterable(allApplicationChanges.entrySet())
                    .flatMap(appChangeEntry -> requestsPlaner.createApplyRequests(appChangeEntry.getKey(),
                            appChangeEntry.getValue()))
                    .onErrorContinue(log::warning);

            log.info("Applying changes to applications");
            applicationRequests.blockLast();
            log.verbose("Changes to space developers applied");
        }
    }

    //TODO update the documentation as soon as the method does more than just creating/removing services
    /**
     * Creates/removes all desired services in your live cf instance.
     * @param desiredServices the services that should all be present in the live system after the procedure
     * @throws ApplyException if an error occurs during the procedure
     * @throws NullPointerException if the argument is null
     */
    public void applyServices(@Nonnull Map<String, ServiceBean> desiredServices) {
        checkNotNull(desiredServices);

        log.info("Fetching services data");
        Map<String, ServiceBean> liveServices = this.getLogic.getServices(servicesOperations);
        log.verbose("Fetching services data completed");

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredServicesConfig = createConfigFromServices(desiredServices);
        ConfigBean liveServicesConfig = createConfigFromServices(liveServices);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        log.info("Diffing services");
        DiffResult diffResult = this.diffLogic.createDiffResult(liveServicesConfig, desiredServicesConfig);
        log.verbose("Diffing space developers completed");

        Map<String, List<CfChange>> allServicesChanges = diffResult.getServiceChanges();

        // TODO: find out how to detect "no changes" condition and log it accordingly
        log.info("Applying changes to applications");
        Flux<Void> serviceRequests = Flux.fromIterable(allServicesChanges.entrySet())
                .flatMap( serviceChangeEntry ->
                        ServiceRequestsPlaner.createApplyRequests(servicesOperations,
                                serviceChangeEntry.getKey(),
                                serviceChangeEntry.getValue()))
                .onErrorContinue(log::warning);
        serviceRequests.blockLast();
        log.verbose("Applying changes to applications completed");
    }

    /**
     * Creates a space with the desired name if a space with such a name does not exist in the live cf instance.
     * @param desiredSpaceName the name of the desired space
     * @throws NullPointerException if the desired paramerter is null
     * @throws ApplyException in case of errors during the creation of the desired space
     * @throws GetException in case of errors during querying the space names
     */
    public void applySpace(String desiredSpaceName) {
        checkNotNull(desiredSpaceName);

        Mono<List<String>> getAllRequest = spaceOperations.getAll();

        log.info("Fetching all spaces' names");
        List<String> spaceNames;
        try {
            spaceNames = getAllRequest.block();
        } catch (Exception e) {
            throw new GetException(e);
        }
        log.verbose("Fetched spaces' names");

        if (!spaceNames.contains(desiredSpaceName)) {
            log.info("Space", desiredSpaceName, "not found, creating");
            Mono<Void> createRequest = spaceOperations.create(desiredSpaceName);
            try {
                createRequest.block();
            } catch (Exception e) {
                throw new ApplyException(e);
            }
            log.verbose("Space", desiredSpaceName, "created");
        } else {
            log.info("Space with name", desiredSpaceName, "already exists");
        }
    }


    /**
     * @param spaceDevelopers the space developer that should be
     *                             contained in the resulting config bean.
     * @return a config bean only containing the entered space developers.
     */
    private ConfigBean createConfigFromSpaceDevelopers(List<String> spaceDevelopers) {
        SpecBean specBean = new SpecBean();
        specBean.setSpaceDevelopers(spaceDevelopers);

        ConfigBean configBean = new ConfigBean();
        configBean.setSpec(specBean);

        return configBean;
    }

    /**
     * @param applicationBeans the application beans that should be contained in the
     *                         resulting config bean
     * @return a config bean only containing the entered application beans
     */
    private ConfigBean createConfigFromApplications(Map<String, ApplicationBean> applicationBeans) {
        SpecBean applicationsSpecBean = new SpecBean();
        applicationsSpecBean.setApps(applicationBeans);
        ConfigBean applicationsConfigBean = new ConfigBean();
        applicationsConfigBean.setSpec(applicationsSpecBean);

        return applicationsConfigBean;
    }

    /**
     * @param serviceBeans the service beans that should be contained in the resulting config bean
     * @return a config bean only containing the entered service beans
     */
    private ConfigBean createConfigFromServices(Map<String, ServiceBean> serviceBeans) {
        SpecBean servicesSpecBean = new SpecBean();
        servicesSpecBean.setServices(serviceBeans);
        ConfigBean servicesConfigBean = new ConfigBean();
        servicesConfigBean.setSpec(servicesSpecBean);
        return servicesConfigBean;
    }

    public void setDiffLogic(DiffLogic diffLogic) {
        this.diffLogic = diffLogic;
    }

    public void setGetLogic(GetLogic getLogic) {
        this.getLogic = getLogic;
    }
}
