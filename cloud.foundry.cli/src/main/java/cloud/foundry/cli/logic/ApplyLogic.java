package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.apply.DiffWrapper;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ApplyLogic {

    private DefaultCloudFoundryOperations cfOperations;

    public ApplyLogic(DefaultCloudFoundryOperations cfOperations) {
        this.cfOperations = cfOperations;
    }

    public void applyApplications(ConfigBean desiredConfig) {
        GetLogic getLogic = new GetLogic(cfOperations);
        ConfigBean liveConfig = getLogic.getAll();

        // compare the entire config as the diff wrapper is only suited for diff trees of the whole config
        DiffLogic diffLogic = new DiffLogic();
        DiffNode diffTreeRoot = diffLogic.createDiffTree(liveConfig, desiredConfig);

        DiffWrapper wrappedDiff = new DiffWrapper(diffTreeRoot);
        Map<String, List<CfChange>> allApplicationChanges = wrappedDiff.getApplicationChanges();

        for (Entry<String, List<CfChange>> applicationChangesEntry : allApplicationChanges.entrySet()) {
            String applicationName = applicationChangesEntry.getKey();
            List<CfChange> applicationChanges = applicationChangesEntry.getValue();

            //TODO apply the changes to the live system using a visitor
        }
    }

}
