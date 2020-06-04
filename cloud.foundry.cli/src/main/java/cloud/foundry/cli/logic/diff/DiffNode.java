package cloud.foundry.cli.logic.diff;

import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Data structure that holds the information of changes in the it's node.
 * It's a composite of diff nodes and acts as tree data structure.
 */
public class DiffNode {

    protected Map<String, DiffNode> childNodes;
    protected List<Change> changes;
    protected String propertyName;

    private DiffNode() {
        this.childNodes = new HashMap<>();
        this.changes = new LinkedList<>();
    }

    public List<Change> getChanges() {
        return new ArrayList<>(changes);
    }

    public Map<String, DiffNode> getChildNodes() {
        return new HashMap<>(childNodes);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void addChild(String propertyName, DiffNode child) {
        this.childNodes.put(propertyName, child);
    }

    public DiffNode getChild(String propertyName) {
        return this.childNodes.get(propertyName);
    }

    public void addChange(Change change) {
        this.changes.add(change);
    }

    public static DiffNode create(String propertyName) {
        DiffNode diffNode = new DiffNode();
        diffNode.setPropertyName(propertyName);
        return diffNode;
    }

    public boolean hasNodeWith(String propertyName) {
        if (this.propertyName.equals(propertyName)) {
            return true;
        }

        for (DiffNode childNode : this.childNodes.values()) {
            if (childNode.hasNodeWith(propertyName)) return true;
        }

        return false;
    }

    private DiffNode getChildWith(String propertyName) {
        for (DiffNode childNode : this.childNodes.values()) {
            if (childNode.getPropertyName().equals(propertyName)) return childNode;
        }

        return null;
    }

    public boolean isLeaf() {
        return this.childNodes.size() == 0;
    }

    public boolean isNewObject() {
        return changes.size() == 1 && changes.get(0) instanceof NewObject;
    }

    public boolean isRemovedObject() {
        return changes.size() == 1 && changes.get(0) instanceof ObjectRemoved;
    }
}
