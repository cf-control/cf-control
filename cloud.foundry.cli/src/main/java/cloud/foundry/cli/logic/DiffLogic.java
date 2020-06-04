package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.NotSupportedChangeType;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.Differ;
import cloud.foundry.cli.logic.diff.output.DiffOutput;

import javax.annotation.Nonnull;

public class DiffLogic {

    public DiffNode createDiffTree(@Nonnull ConfigBean liveConfig,@Nonnull ConfigBean desiredConfig) {
        return Differ.createDiffTree(liveConfig, desiredConfig);
    }

    public String createDiffOutput(@Nonnull ConfigBean liveConfig,@Nonnull ConfigBean desiredConfig)
            throws NotSupportedChangeType {
        DiffNode diffNode = Differ.createDiffTree(liveConfig, desiredConfig);
        DiffOutput diffOutput = new DiffOutput();
        return diffOutput.from(diffNode);
    }
}
