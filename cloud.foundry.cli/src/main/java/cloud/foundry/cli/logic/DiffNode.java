package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.logic.output.DiffStringUtils;
import cloud.foundry.cli.logic.output.FlagSymbol;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.map.MapChange;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO doc
 */
public class DiffNode {

    private static final int DEFAULT_INDENTATION = 2;

    protected Map<String, DiffNode> childNodes;
    protected List<Change> changes;
    protected String propertyName;
    private int indentation;

    private DiffNode() {
        this.childNodes = new HashMap<>();
        this.changes = new LinkedList<>();
        indentation = DEFAULT_INDENTATION;
    }

    /**
     * API method
     * @return
     */
    public void setIndentation(int indentation) {
        this.indentation = indentation;
    }

    public String getPropertyName() {
        return propertyName;
    }

    /**
     * API method
     * @return
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * API method
     * @return
     */
    public void addChild(String propertyName, DiffNode child) {
        this.childNodes.put(propertyName, child);
    }

    /**
     * API method
     * @return
     */
    public static DiffNode create(String rootProperty) {
        DiffNode diffNode = new DiffNode();
        diffNode.setPropertyName(rootProperty);
        return diffNode;
    }

    /**
     * API method
     * @return
     */
    public DiffNode insert(LinkedList<String> path, Change change) {
        if (path.size() == 0) {
            this.changes.add(change);
            return this;
        }

        String propertyName = path.removeFirst();
        if (!propertyName.equals(this.propertyName)) {
            throw new IllegalArgumentException("not a valid path");
        }

        if (path.size() > 0) {
            String childProperty = path.getFirst();
            if (childNodes.get(childProperty) == null) {
                childNodes.put(childProperty, new DiffNode());
            }

            DiffNode childNode = childNodes.get(childProperty);
            childNode.setPropertyName(childProperty);
            childNode.insert(path, change);
            this.addChild(childNode.propertyName, childNode);
        } else {
            this.changes.add(change);
        }

        return this;
    }

    /**
     * API method
     * @return
     */
    public String toDiffString() {
        return this.toDiffString(this.indentation);
    }

    private String toDiffString(int indentation) {
        DiffStringUtils diffStringBuilder = new DiffStringUtils();

        //no changes at this level which means, there are no changes at the sub-levels also
        if (changes.size() == 0 && isLeaf()) return "";

        if (isNewObject()) {
            return diffStringBuilder.fromBean(FlagSymbol.ADDED,
                    indentation,
                    (Bean) changes.get(0).getAffectedObject().get(),
                    propertyName);
        } else if (isRemovedObject()) {
            return diffStringBuilder.fromBean(FlagSymbol.REMOVED,
                    indentation,
                    (Bean) changes.get(0).getAffectedObject().get(),
                    propertyName);
        } else {

            StringBuilder sb = new StringBuilder();
            sb.append(diffStringBuilder.fromProperty(FlagSymbol.NONE, indentation - 2, propertyName));

            for (Change change : changes) {
                if (change instanceof MapChange && !isLeaf()) continue;

                sb.append(diffStringBuilder.fromChange(indentation, change));
            }

            for (DiffNode childNode : childNodes.values()) {
                sb.append(childNode.toDiffString(2 + indentation));
            }
            return sb.toString();
        }
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

    public void removeNode(String propertyName) {
        if (this.propertyName.equals(propertyName)) {
            throw new IllegalArgumentException();
        }

        if (childNodes.containsKey(propertyName)) {
            childNodes.remove(propertyName);
            return;
        }

        for (DiffNode childNode : this.childNodes.values()) {
            childNode.removeNode(propertyName);
        }
    }

    private DiffNode getChildWith(String propertyName) {
        for (DiffNode childNode : this.childNodes.values()) {
            if (childNode.getPropertyName().equals(propertyName)) return childNode;
        }

        return null;
    }

    protected boolean isLeaf() {
        return this.childNodes.size() == 0;
    }

    protected boolean isNewObject() {
        return changes.size() == 1 && changes.get(0) instanceof NewObject;
    }

    protected boolean isRemovedObject() {
        return changes.size() == 1 && changes.get(0) instanceof ObjectRemoved;
    }
}
