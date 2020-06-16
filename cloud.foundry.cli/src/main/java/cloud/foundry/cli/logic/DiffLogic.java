package cloud.foundry.cli.logic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.DiffException;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.Differ;
import cloud.foundry.cli.logic.diff.output.DiffOutput;

/**
 * Handles the operations to compare the configuration of a cloud foundry instance with a different configuration.
 */
public class DiffLogic {

    private static final String BEANS_DONT_MATCH_ERROR = "Bean types don't match.";

    /**
     * Compares the two given configurations and creates a tree composed of @DiffNode objects.
     * @param liveConfig the configuration that is currently on the live system
     * @param desiredConfig the configuration state that the live system should change to
     * @return @DiffResult object that provides access to the found changes
     * @throws NullPointerException when liveConfig or desiredConfig is null
     * @throws IllegalArgumentException when the two beans don't have the same type
     * @throws DiffException in case of any errors during the diff procedure
     */
    public DiffResult createDiffResult(Bean liveConfig, Bean desiredConfig) {
        checkNotNull(liveConfig);
        checkNotNull(desiredConfig);
        checkArgument(liveConfig.getClass() == desiredConfig.getClass(), BEANS_DONT_MATCH_ERROR);

        try {
            return doCreateDiffResult(liveConfig, desiredConfig);
        } catch (Exception e) {
            throw new DiffException(e.getMessage(), e);
        }
    }

    private DiffResult doCreateDiffResult(Bean liveConfig, Bean desiredConfig) {
        Differ differ = new Differ();
        return new DiffResult(differ.createDiffTree(liveConfig, desiredConfig));
    }

    /**
     * Compares the two given configurations and creates a string representation of the differences.
     * @param liveConfig the configuration that is currently on the live system
     * @param desiredConfig the configuration state that the live system should change to
     * @return a visually enhanced diff output as string
     * @throws NullPointerException when liveConfig or desiredConfig is null
     * @throws IllegalArgumentException when the two beans don't have the same type
     * @throws DiffException in case of any errors during the diff procedure
     */
    public String createDiffOutput(Bean liveConfig, Bean desiredConfig) {
        checkNotNull(liveConfig);
        checkNotNull(desiredConfig);
        checkArgument(liveConfig.getClass() == desiredConfig.getClass(), BEANS_DONT_MATCH_ERROR);

        try {
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
