package cloud.foundry.cli.logic.diff;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeParser;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class compares two given beans of the same type and builds a DiffNode tree data structure
 * with differences found between the bean objects.
 * Javers stores changes in a Change class, which holds information about the absolute path
 * from the Root Object (ConfigBean here) to the actual level where a change has taken place.
 * The idea is to build a tree data structure where each node holds the changes of their level.
 */
public class Differ {

    private static final Javers JAVERS = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
            .build();

    /**
     * Compares the two given configurations and creates a tree composed of @DiffNode objects.
     * @param liveConfig the configuration that is currently on the live system
     * @param desiredConfig the configuration state that the live system should change to
     * @return @DiffNode object which is the root of the tree
     * @throws NullPointerException when liveConfig is null
     *  when desiredConfig is null
     * @throws IllegalArgumentException when the two beans don't have the same type
     */
    public DiffNode createDiffTree(Bean liveConfig, Bean desiredConfig) {
        checkNotNull(liveConfig);
        checkNotNull(desiredConfig);
        checkArgument(liveConfig.getClass() == desiredConfig.getClass(), "Bean types don't match.");

        return doCreateDiffTree(liveConfig, desiredConfig);
    }

    private DiffNode doCreateDiffTree(Bean liveConfig, Bean desiredConfig) {
        Diff diff = JAVERS.compare(liveConfig, desiredConfig);

        // parse the change objects created by the JaVers diff to custom change objects
        List<CfChange> cfChanges = diff.getChanges()
                .stream()
                .map(ChangeParser::parse)
                // Change types that are not relevant to us will get parsed to null, so ignore them
                .filter(Objects::nonNull)
                // As of the specification, removed object nodes shouldn't be displayed.
                // TODO make it configurable
                .filter(change -> !(change instanceof CfRemovedObject))
                // Always remove inner maps for now, since they clash with the output algorithm of DiffOutput.
                // SpecBean contains the only inner maps
                //TODO make it configurable
                .filter(change -> !(change instanceof CfMapChange && change.getAffectedObject() instanceof SpecBean))
                .collect(Collectors.toList());

        return DiffTreeCreator.createFrom(cfChanges);
    }

}
