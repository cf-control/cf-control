package cloud.foundry.cli.logic.diff;

import static com.google.common.base.Preconditions.checkArgument;

import org.javers.core.diff.Change;

import javax.annotation.Nonnull;
import java.util.LinkedList;

public class DiffTreeCreator {


    /**
     * API method
     * @return
     */
    public static void insert(@Nonnull DiffNode rootNode,@Nonnull LinkedList<String> path, @Nonnull Change change) {
        checkArgument(!path.isEmpty(), "The path is empty");
        checkArgument(rootNode.getPropertyName().equals(path.getFirst()),
                "The root node is not the first node of the path");

        doInsert(rootNode, path, change);
    }

    private static void doInsert(DiffNode node, LinkedList<String> path, Change change) {
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
