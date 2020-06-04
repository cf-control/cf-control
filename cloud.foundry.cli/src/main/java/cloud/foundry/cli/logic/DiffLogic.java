package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.NotSupportedChangeType;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.Differ;
import cloud.foundry.cli.logic.diff.output.DiffOutput;

public class DiffLogic {

    public DiffNode createDiffTree(ConfigBean liveConfig, ConfigBean desiredConfig) {
        return Differ.createDiffTree(liveConfig, desiredConfig);
    }

    public String createDiffOutput(ConfigBean liveConfig, ConfigBean desiredConfig) throws NotSupportedChangeType {
        DiffNode diffNode = Differ.createDiffTree(liveConfig, desiredConfig);
        DiffOutput diffOutput = new DiffOutput();
        return diffOutput.from(diffNode);
    }
}
