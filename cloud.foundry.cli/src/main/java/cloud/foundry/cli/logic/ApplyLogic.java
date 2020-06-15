package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.ApplyExcpetion;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.apply.AppApplyResolver;
import cloud.foundry.cli.logic.apply.DiffWrapper;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ApplyLogic {

    private DefaultCloudFoundryOperations cfOperations;

    public ApplyLogic(DefaultCloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }

    public void applyApplications(Map<String, ApplicationBean> desiredApplications) throws ApplyExcpetion {
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        Map<String, ApplicationBean> liveApplications = applicationsOperations.getAll().block();

        // that way only the applications of the live system are compared in the diff
        ConfigBean desiredApplicationsConfig = new ConfigBean(desiredApplications);
        ConfigBean liveApplicationsConfig = new ConfigBean(liveApplications);

        // compare entire configs as the diff wrapper is only suited for diff trees of these
        DiffLogic diffLogic = new DiffLogic();
        DiffNode diffTreeRoot = diffLogic.createDiffTree(liveApplicationsConfig, desiredApplicationsConfig);
        DiffWrapper wrappedDiff = new DiffWrapper(diffTreeRoot);

        Map<String, List<CfChange>> allApplicationChanges = wrappedDiff.getApplicationChanges();

        for (Entry<String, List<CfChange>> applicationChangesEntry : allApplicationChanges.entrySet()) {
            String applicationName = applicationChangesEntry.getKey();
            Log.debug("Start applying the changes to the app:", applicationName);
            List<CfChange> applicationChanges = applicationChangesEntry.getValue();

            AppApplyResolver appApplyResolver = new AppApplyResolver(cfOperations, applicationName);
            appApplyResolver.applyOnAppChanges(applicationChanges);
        }
    }

}
