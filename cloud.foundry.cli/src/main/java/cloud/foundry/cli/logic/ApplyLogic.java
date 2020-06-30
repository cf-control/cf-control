package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.logic.apply.ApplicationRequestsPlaner;
import cloud.foundry.cli.logic.apply.SpaceDevelopersRequestsPlaner;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Flux;

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

    /**
     * Creates a new instance that will use the provided cf operations internally.
     *
     * @param cfOperations the cf operations that should be used to communicate with
     *                     the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
    }

    /**
     * Assign users as space developers that are not present in the live system and
     * revoke space developers permission, if its present in the live system but not
     * defined in <code>desiredSpaceDevelopers</code>. In case of any error, the
     * procedure will be continued and the error is logged.
     *
     * @param desiredSpaceDevelopers the space developers that should all be present
     *                               in the live system after the procedure.
     * @throws NullPointerException if the argument is null.
     */
    public void applySpaceDevelopers(@Nonnull List<String> desiredSpaceDevelopers) {
        checkNotNull(desiredSpaceDevelopers);

        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        log.info("Fetching information about space developers...");
        GetLogic getLogic = new GetLogic();
        List<String> liveSpaceDevelopers = getLogic.getSpaceDevelopers(spaceDevelopersOperations);
        // List<String> liveSpaceDevelopers = spaceDevelopersOperations.getAll().block();
        log.info("Information fetched.");

        ConfigBean desiredSpaceDevelopersConfig = createConfigFromSpaceDevelopers(desiredSpaceDevelopers);
        ConfigBean liveSpaceDevelopersConfig = createConfigFromSpaceDevelopers(liveSpaceDevelopers);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        DiffLogic diffLogic = new DiffLogic();
        log.info("Comparing the space developers...");
        DiffResult wrappedDiff = diffLogic.createDiffResult(liveSpaceDevelopersConfig, desiredSpaceDevelopersConfig);
        log.info("Space developers compared.");

        CfContainerChange spaceDevelopersChange = wrappedDiff.getSpaceDevelopersChange();
        if (spaceDevelopersChange == null) {
            log.info("There is nothing to apply");
        } else {
            Flux<Void> spaceDevelopersRequests = Flux.just(spaceDevelopersChange)
                    .flatMap(spaceDeveloperChange -> SpaceDevelopersRequestsPlaner
                            .createSpaceDevelopersRequests(spaceDevelopersOperations, spaceDeveloperChange))
                    .onErrorContinue(log::warning);
            log.info("Applying changes to space developers...");
            spaceDevelopersRequests.blockLast();
        }
    }

    // TODO update the documentation as soon as the method does more than just
    // creating applications

    /**
     * Creates all provided applications that are not present in the live system. In
     * case of any error, the procedure is discontinued.
     *
     * @param desiredApplications the applications that should all be present in the
     *                            live system after the procedure
     * @throws ApplyException       if an error occurs during the procedure
     * @throws NullPointerException if the argument is null
     */
    public void applyApplications(@Nonnull Map<String, ApplicationBean> desiredApplications) {
        checkNotNull(desiredApplications);

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        log.info("Fetching information about applications...");
        Map<String, ApplicationBean> liveApplications = applicationsOperations.getAll().block();
        log.info("Information fetched.");

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredApplicationsConfig = createConfigFromApplications(desiredApplications);
        ConfigBean liveApplicationsConfig = createConfigFromApplications(liveApplications);

        // compare entire configs as the diff wrapper is only suited for diff trees of
        // these
        DiffLogic diffLogic = new DiffLogic();
        log.info("Comparing the applications...");
        DiffResult wrappedDiff = diffLogic.createDiffResult(liveApplicationsConfig, desiredApplicationsConfig);
        log.info("Applications compared.");

        Map<String, List<CfChange>> allApplicationChanges = wrappedDiff.getApplicationChanges();
        ApplicationsOperations appOperations = new ApplicationsOperations(cfOperations);

        Flux<Void> applicationRequests = Flux.fromIterable(allApplicationChanges.entrySet())
                .flatMap(appChangeEntry -> ApplicationRequestsPlaner.create(appOperations, appChangeEntry.getKey(),
                        appChangeEntry.getValue()))
                .onErrorContinue(log::warning);
        applicationRequests.blockLast();

        log.info("Applying changes to applications...");
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

}
