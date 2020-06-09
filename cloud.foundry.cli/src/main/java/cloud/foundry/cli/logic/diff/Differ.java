package cloud.foundry.cli.logic.diff;

import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeParser;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.ObjectRemoved;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * the classes serves as the main module for creating the difference between two configurations
 */
public class Differ {

    private static final Javers JAVERS = JaversBuilder.javers()
            .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
            .build();
    private static final String ROOT_SYMBOL = "#";
    private static final String PATH_SEPARATOR_SYMBOL = "/";
    private static final String ROOT_NAME = "root";

    /**
     * compares the two given configurations and creates a tree composed of @DiffNode objects
     * @param liveConfig the configuration that is currently on the live system
     * @param desiredConfig the configuration state that the live system should change to
     * @return @DiffNode objects which is the root of the tree
     */
    public DiffNode createDiffTree(Bean liveConfig, Bean desiredConfig) {
        return doCreateDiffTree(liveConfig, desiredConfig);
    }

    private DiffNode doCreateDiffTree(Bean liveConfig, Bean desiredConfig) {
        Diff diff = JAVERS.compare(liveConfig, desiredConfig);

        DiffNode diffNode = new DiffNode(ROOT_NAME);
        for (Change change: diff.getChanges()) {
            //TODO wrap change in custom change object
            if (change instanceof ObjectRemoved) continue;

            CfChange cfChange = ChangeParser.parse(change);
            if (cfChange == null) continue;

            LinkedList<String> path = extractPath(change);
            replaceRoot(ROOT_NAME, path);
            DiffTreeCreator.insert(diffNode, path, cfChange);
        }

        return diffNode;
    }

    /**
     * for example:
     * change.getAffectedGlobalId() = cloud.foundry.cli.crosscutting.bean.ConfigBean/#spec/apps/someApp/manifest
     *                           -> [cloud.foundry.cli.crosscutting.bean.ConfigBean, spec, apps, someApp, manifest]
     */
    private static LinkedList<String> extractPath(Change change) {
        return new LinkedList<>(Arrays.asList(change
                .getAffectedGlobalId()
                .toString()
                .replace(ROOT_SYMBOL, "")
                .split(PATH_SEPARATOR_SYMBOL)));
    }

    /**
     * example: from [cloud.foundry.cli.crosscutting.bean.ConfigBean, spec, apps, someApp, manifest]
     *          to [{propertyName}, spec, apps, someApp, manifest]
     */
    private static void replaceRoot(String propertyName, LinkedList<String> path) {
        path.removeFirst();
        path.addFirst(propertyName);
    }

}
