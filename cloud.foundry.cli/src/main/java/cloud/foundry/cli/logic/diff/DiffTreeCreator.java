package cloud.foundry.cli.logic.diff;

import org.javers.core.diff.Change;

import java.util.LinkedList;

public class DiffTreeCreator {


    /**
     * API method
     * @return
     */
    public DiffNode insert(DiffNode node, LinkedList<String> path, Change change) {
        if (path.size() == 0) {
            node.addChange(change);
            return node;
        }

        String propertyName = path.removeFirst();
        if (!propertyName.equals(node.getPropertyName())) {
            throw new IllegalArgumentException("not a valid path");
        }

        if (path.size() > 0) {
            String childProperty = path.getFirst();

            if (node.getChild(childProperty) == null) {
                node.addChild(childProperty, new DiffNode(childProperty));
            }

            DiffNode childNode = node.getChild(childProperty);
            insert(childNode, path, change);
            node.addChild(childNode.propertyName, childNode);
        } else {
            node.addChange(change);
        }

        return node;
    }

}
