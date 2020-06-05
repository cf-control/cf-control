package cloud.foundry.cli.logic.diff;

import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;

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

    protected DiffNode parentNode;
    protected Map<String, DiffNode> childNodes;
    //TODO use custom wrapper for the change object
    protected List<Change> changes;
    protected String propertyName;

    //TODO helper method isRoot()

    public DiffNode(@Nonnull String propertyName) {
        this.parentNode = null;
        this.propertyName = propertyName;
        this.childNodes = new HashMap<>();
        this.changes = new LinkedList<>();
    }

    public DiffNode(@Nonnull String propertyName, @Nonnull DiffNode parentNode) {
        this.parentNode = parentNode;
        this.propertyName = propertyName;
        this.childNodes = new HashMap<>();
        this.changes = new LinkedList<>();
    }

    public DiffNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(DiffNode parentNode) {
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
    public Map<String, DiffNode> getChildNodes() {
        return Collections.unmodifiableMap(childNodes);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void addChild(@Nonnull String propertyName,@Nonnull DiffNode child) {
        this.childNodes.put(propertyName, child);
    }

    public DiffNode getChild(@Nonnull String propertyName) {
        return this.childNodes.get(propertyName);
    }

    public void addChange(@Nonnull Change change) {
        this.changes.add(change);
    }

    //TODO move into wrapper class
    public boolean hasNodeWith(@Nonnull String propertyName) {
        if (this.propertyName.equals(propertyName)) {
            return true;
        }

        for (DiffNode childNode : this.childNodes.values()) {
            if (childNode.hasNodeWith(propertyName)) return true;
        }

        return false;
    }

    //TODO move into wrapper class
    private DiffNode getChildWith(@Nonnull String propertyName) {
        for (DiffNode childNode : this.childNodes.values()) {
            if (childNode.getPropertyName().equals(propertyName)) return childNode;
        }

        return null;
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
