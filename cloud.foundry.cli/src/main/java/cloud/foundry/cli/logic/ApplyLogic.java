package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.logic.apply.ApplicationRequestsPlaner;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * This class takes care of applying desired cloud foundry configurations to a live system.
 */
public class ApplyLogic {

    private DefaultCloudFoundryOperations cfOperations;

    /**
     * Creates a new instance that will use the provided cf operations internally.
     * @param cfOperations the cf operations that should be used to communicate with the cf instance
     * @throws NullPointerException if the argument is null
     */
    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
    }

    //TODO update the documentation as soon as the method does more than just creating applications
    /**
     * Creates all provided applications that are not present in the live system. In case of any error, the procedure
     * is discontinued.
     * @param desiredApplications the applications that should all be present in the live system after the procedure
     * @throws ApplyException if an error occurs during the procedure
     * @throws NullPointerException if the argument is null
     */
    public void applyApplications(@Nonnull Map<String, ApplicationBean> desiredApplications) {
        checkNotNull(desiredApplications);

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        Log.info("Fetching information about applications...");
        Map<String, ApplicationBean> liveApplications = applicationsOperations.getAll().block();
        Log.info("Information fetched.");

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredApplicationsConfig = createConfigFromApplications(desiredApplications);
        ConfigBean liveApplicationsConfig = createConfigFromApplications(liveApplications);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        DiffLogic diffLogic = new DiffLogic();
        Log.info("Comparing the applications...");
        DiffResult wrappedDiff = diffLogic.createDiffResult(liveApplicationsConfig, desiredApplicationsConfig);
        Log.info("Applications compared.");

        Map<String, List<CfChange>> allApplicationChanges = wrappedDiff.getApplicationChanges();
        ApplicationsOperations appOperations = new ApplicationsOperations(cfOperations);

        Flux<Void> applicationRequests = Flux.fromIterable(allApplicationChanges.entrySet())
                .flatMap( appChangeEntry -> ApplicationRequestsPlaner.create(appOperations, appChangeEntry.getKey(),
                        appChangeEntry.getValue()))
                .doOnSubscribe(subscription -> System.out.println(Thread.currentThread().getName()))
                .onErrorContinue((throwable, o) -> throwable.printStackTrace());
        applicationRequests.blockLast();

        Log.info("Applying changes to applications...");
    }

    /**
     * @param applicationBeans the application beans that should be contained in the resulting config bean
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
