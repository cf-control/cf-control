package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.apply.ApplicationApplier;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class ApplyLogic {

    private DefaultCloudFoundryOperations cfOperations;

    public ApplyLogic(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
    }

    public void applyApplications(@Nonnull Map<String, ApplicationBean> desiredApplications) {
        checkNotNull(desiredApplications);

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        Map<String, ApplicationBean> liveApplications = applicationsOperations.getAll().block();

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredApplicationsConfig = createConfigFromApplications(desiredApplications);
        ConfigBean liveApplicationsConfig = createConfigFromApplications(liveApplications);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        DiffLogic diffLogic = new DiffLogic();
        DiffResult wrappedDiff = diffLogic.createDiffResult(liveApplicationsConfig, desiredApplicationsConfig);

        Map<String, List<CfChange>> allApplicationChanges = wrappedDiff.getApplicationChanges();

        for (Entry<String, List<CfChange>> applicationChangesEntry : allApplicationChanges.entrySet()) {
            String applicationName = applicationChangesEntry.getKey();
            Log.debug("Start applying the changes to the app:", applicationName);
            List<CfChange> applicationChanges = applicationChangesEntry.getValue();

            ApplicationApplier.apply(new ApplicationsOperations(cfOperations), applicationName, applicationChanges);
        }
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
