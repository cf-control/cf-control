package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.beans.SpecBean;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;

import java.util.Arrays;
import java.util.LinkedList;


/**
 * TODO doc
 */
public class Differ {

    private static final Javers JAVERS = JaversBuilder.javers().build();
    private static final String ROOT_SYMBOL = "#";
    private static final String PATH_SEPARATOR_SYMBOL = "/";
    private static final String ROOT_NAME = "root";

    /**
     * TODO doc
     */
    public static DiffNode createDiffTree(ConfigBean liveConfig, ConfigBean desiredConfig) {
        /**
         * Javers stores changes in a Change class, which holds information about the absolute path
         * from the Root Object (ConfigBean here) to the actual level where a change has taken place.
         * The idea is to build a tree data structure where each node holds the changes of their level.
         */

        Diff diff = JAVERS.compare(liveConfig, desiredConfig);

        DiffNode diffNode = DiffNode.create(ROOT_NAME);
        for (Change change: diff.getChanges()) {
           if (change instanceof ObjectRemoved) continue;

            LinkedList<String> path = extractPath(change);
            replaceRoot(ROOT_NAME, path);
            diffNode.insert(path, change);

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
