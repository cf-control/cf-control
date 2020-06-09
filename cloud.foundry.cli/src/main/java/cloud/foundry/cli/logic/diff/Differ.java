package cloud.foundry.cli.logic.diff;

import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeParser;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * the classes serves as the main module for creating the difference between two configurations
 */
public class Differ {

    /**
     * Javers stores changes in a Change class, which holds information about the absolute path
     * from the Root Object (ConfigBean here) to the actual level where a change has taken place.
     * The idea is to build a tree data structure where each node holds the changes of their level.
     */

    private static final Javers JAVERS = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
            .build();
    /**
     * compares the two given configurations and creates a tree composed of @DiffNode objects
     * @param liveConfig the configuration that is currently on the live system
     * @param desiredConfig the configuration state that the live system should change to
     * @return @DiffNode objects which is the root of the tree
     */
    public DiffNode createDiffTree(Bean liveConfig, Bean desiredConfig) {
        checkNotNull(liveConfig);
        checkNotNull(desiredConfig);

        return doCreateDiffTree(liveConfig, desiredConfig);
    }

    private DiffNode doCreateDiffTree(Bean liveConfig, Bean desiredConfig) {
        Diff diff = JAVERS.compare(liveConfig, desiredConfig);

        diff.getChanges().forEach(change -> {
            System.out.println(change.getAffectedGlobalId());
            System.out.println(change.getClass());
        });

        List<CfChange> cfChanges = diff.getChanges()
                .stream()
                // parse to custom change object
                .map(ChangeParser::parse)
                // null when javers change object is not parsable, but not parsable objects are irrelevant to us
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        DiffNode diffNode = new DiffNode("config");
        for(CfChange cfChange : cfChanges) {
            DiffTreeCreator.insert(diffNode, new LinkedList<>(cfChange.getPath()), cfChange);
        }

        removeAllMapsAndContainersNotAtLeaf(diffNode);
        return diffNode;
    }

    private void removeAllMapsAndContainersNotAtLeaf(DiffNode node) {
        if(!node.isLeaf()) {
            node.setChanges(node
                    .getChanges()
                    .stream()
                    .filter(change -> !(change instanceof CfContainerChange) && !(change instanceof CfMapChange))
                    .collect(Collectors.toList())
            );
        }

        for(DiffNode childNode: node.getChildNodes()){
            removeAllMapsAndContainersNotAtLeaf(childNode);
        }
    }
}
