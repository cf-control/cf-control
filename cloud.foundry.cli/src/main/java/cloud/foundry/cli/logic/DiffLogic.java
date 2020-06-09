package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.DiffException;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.Differ;
import cloud.foundry.cli.logic.diff.output.DiffOutput;

public class DiffLogic {

    /**
     *
     * @param liveConfig
     * @param desiredConfig
     * @return
     * @throws DiffException
     */
    public DiffNode createDiffTree(Bean liveConfig, Bean desiredConfig) throws DiffException {
        try {
            checkNotNull(liveConfig);
            checkNotNull(desiredConfig);
            checkArgument(liveConfig.getClass() == desiredConfig.getClass(), "Bean types don't match.");

            return doCreateDiffTree(liveConfig, desiredConfig);
        } catch (Exception e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    private DiffNode doCreateDiffTree(Bean liveConfig, Bean desiredConfig) {
        Differ differ = new Differ();
        return differ.createDiffTree(liveConfig, desiredConfig);
    }

    /**
     *
     * @param liveConfig
     * @param desiredConfig
     * @return
     * @throws DiffException
     */
    public String createDiffOutput(Bean liveConfig, Bean desiredConfig) throws DiffException {
        try {
            checkNotNull(liveConfig);
            checkNotNull(desiredConfig);
            checkArgument(liveConfig.getClass() == desiredConfig.getClass(), "Bean types don't match.");

            return doCreateDiffOutput(liveConfig, desiredConfig);
        } catch (Exception e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    private String doCreateDiffOutput(Bean liveConfig, Bean desiredConfig) {
        Differ differ = new Differ();
        DiffNode diffNode = differ.createDiffTree(liveConfig, desiredConfig);
        DiffOutput diffOutput = new DiffOutput();
        return diffOutput.from(diffNode);
    }
}
