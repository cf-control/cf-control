package cloud.foundry.cli.logic.diff;

import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Data structure that holds the information of changes in the it's node.
 * It's a composite of diff nodes and acts as tree data structure.
 */
public class DiffNode {

    protected DiffNode parentNode;
    protected Map<String, DiffNode> childNodes;
    //TODO use custom wrapper for the change object
    protected List<Change> changes;
    final protected String propertyName;

    public DiffNode(@Nonnull String propertyName) {
        this.parentNode = null;
        this.propertyName = propertyName;
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
     * TODO immutable
     * @return
     */
    public List<Change> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    /**
     * TODO immutable
     * @return
     */
    public Collection<DiffNode> getChildNodes() {
        return Collections.unmodifiableCollection(childNodes.values());
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void addChild(@Nonnull DiffNode child) {
        this.childNodes.put(child.getPropertyName(), child);
        child.setParentNode(this);
    }

    public DiffNode getChild(@Nonnull String propertyName) {
        return this.childNodes.get(propertyName);
    }

    public void addChange(@Nonnull Change change) {
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
        return changes.size() == 1 && changes.get(0) instanceof NewObject;
    }

    public boolean isRemovedObject() {
        return changes.size() == 1 && changes.get(0) instanceof ObjectRemoved;
    }
}
