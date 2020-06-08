package cloud.foundry.cli.logic.diff;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible for creating a diff tree consisting of {@link DiffNode diff nodes} from a number of
 * {@link CfChange change objects}.
 */
public class DiffTreeCreator {

    /**
     * Creates the a diff tree based on the given changes.
     * Uses the path set in every change object to build the tree structure.
     * @param changes list of changes
     * @return DiffNode root node of the tree
     * @throws NullPointerException when changes is null
     */
    public static DiffNode createFrom(List<CfChange> changes) {
        checkNotNull(changes);

        if (changes.size() > 0) {
            // the first entry of every node is the root name
            String rootName = changes.get(0).getPath().get(0);

            DiffNode diffNode = new DiffNode(rootName);
            for (CfChange change : changes) {
                DiffTreeCreator.insert(diffNode, new LinkedList<>(change.getPath()), change);
            }
            return diffNode;
        }
        return new DiffNode("");
    }

    /**
     * API method
     * @return
     */
    public static void insert(@Nonnull DiffNode rootNode,@Nonnull LinkedList<String> path, @Nonnull CfChange change) {
        checkNotNull(rootNode);
        checkNotNull(path);
        checkNotNull(change);
        checkArgument(!path.isEmpty(), "The path is empty");
        checkArgument(rootNode.getPropertyName().equals(path.getFirst()),
                "The root node is not the first node of the path");

        doInsert(rootNode, path, change);
    }

    private static void doInsert(DiffNode node, LinkedList<String> path, CfChange change) {
        String propertyName = path.removeFirst();
        assert propertyName.equals(node.getPropertyName());

        if (path.isEmpty()) {
            node.addChange(change);
            return;
        }

        String childProperty = path.getFirst();

        DiffNode childNode = node.getChild(childProperty);
        if (childNode == null) {
            childNode = new DiffNode(childProperty);
            node.addChild(childNode);
        }

        insert(childNode, path, change);
    }

}
