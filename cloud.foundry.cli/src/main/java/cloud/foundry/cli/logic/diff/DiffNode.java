package cloud.foundry.cli.logic.diff;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Data structure that holds the information of changes in the it's node.
 * It's a composite of diff nodes and acts as tree data structure.
 */
public class DiffNode {

    protected final String propertyName;
    protected DiffNode parentNode;
    protected Map<String, DiffNode> childNodes;
    protected List<CfChange> changes;

    /**
     * @param propertyName the name of the property that is affected by the changes of this node
     */
    public DiffNode(@Nonnull String propertyName) {
        checkNotNull(propertyName);
        this.propertyName = propertyName;
        this.parentNode = null;
        this.childNodes = new HashMap<>();
        this.changes = new LinkedList<>();
    }

    public DiffNode getParentNode() {
        return parentNode;
    }

    private void setParentNode(DiffNode parentNode) {
        this.parentNode = parentNode;
    }

    public List<CfChange> getChanges() {
        return changes;
    }

    /**
     * @return a collection holding all child nodes of this node
     */
    public Map<String, DiffNode> getChildNodes() {
        return Collections.unmodifiableMap(childNodes);
    }

    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Adds the node as a child of this node.
     * @param child the child to add
     * @throws NullPointerException if the argument is null
     */
    public void addChild(DiffNode child) {
        checkNotNull(child);

        this.childNodes.put(child.getPropertyName(), child);
        child.setParentNode(this);
    }

    /**
     * Gets a child node by its property name.
     * @param propertyName property name of the child
     * @return the child node with the specified property name or null if there is no such child node
     * @throws NullPointerException if the argument is null
     */
    public DiffNode getChild(String propertyName) {
        checkNotNull(propertyName);

        return this.childNodes.get(propertyName);
    }

    /**
     * Adds the change to the changes of this node.
     * @param change the change to add
     * @throws NullPointerException if the argument is null
     */
    public void addChange(CfChange change) {
        checkNotNull(change);

        this.changes.add(change);
    }

    /**
     * @return whether this node is a leaf node (i.e. whether it has no child nodes)
     */
    public boolean isLeaf() {
        return this.childNodes.size() == 0;
    }

    /**
     * @return whether this node is a root node (i.e. whether it has no parent node)
     */
    public boolean isRoot() {
        return this.parentNode == null;
    }

    /**
     * Determines the depth of this node in the whole tree. A root node returns a depth of 0.
     * @return the depth of this node
     */
    public int getDepth() {
        if (this.isRoot()) {
            return 0;
        }

        return 1 + this.parentNode.getDepth();
    }

    /**
     * @return whether this node holds a single change, which tells that a new object was added.
     */
    public boolean isNewObject() {
        return changes.size() == 1 &&
                changes.get(0) instanceof CfNewObject;
    }

    /**
     * @return whether this node holds a single change, which tells that an object was removed.
     */
    public boolean isRemovedObject() {
        return changes.size() == 1 &&
                changes.get(0) instanceof CfRemovedObject;
    }
}
