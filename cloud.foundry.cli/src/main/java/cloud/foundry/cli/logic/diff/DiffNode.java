package cloud.foundry.cli.logic.diff;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;

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

    /**
     * @return
     */
    public List<CfChange> getChanges() {
        return changes;
    }

    public void setChanges(List<CfChange> changes) {
        checkNotNull(changes);

        this.changes = changes;
    }

    /**
     * @return
     */
    public Map<String, DiffNode> getChildNodes() {
        return Collections.unmodifiableMap(childNodes);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void addChild(DiffNode child) {
        checkNotNull(child);

        this.childNodes.put(child.getPropertyName(), child);
        child.setParentNode(this);
    }

    public DiffNode getChild(String propertyName) {
        checkNotNull(propertyName);

        return this.childNodes.get(propertyName);
    }

    public void addChange(CfChange change) {
        checkNotNull(change);

        this.changes.add(change);
    }

    public boolean isLeaf() {
        return this.childNodes.size() == 0;
    }

    public boolean isRoot() {
        return this.parentNode == null;
    }

    public int getDepth() {
        if (this.isRoot()) {
            return 0;
        }

        return 1 + this.parentNode.getDepth();
    }

    public boolean isNewObject() {
        return changes.size() == 1 &&
                changes.get(0) instanceof CfNewObject;
    }

    public boolean isRemovedObject() {
        return changes.size() == 1 &&
                changes.get(0) instanceof CfRemovedObject;
    }
}
