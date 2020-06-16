package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.logic.apply.CfChangeVisitor;
import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.List;

/**
 * Holds the information that an object was removed.
 */
public class CfRemovedObject extends CfChange {

    /**
     * @param affectedObject the object that holds the removed object as a field
     * @param propertyName the field name of the removed object
     * @param path the field names of the object graph that lead to removed object (with the compared object as root)
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the path does not contain a root (i.e. if the path is empty)
     */
    public CfRemovedObject(Object affectedObject, String propertyName, List<String> path) {
        super(affectedObject, propertyName, path);
    }

    /**
     * Accept a visitor handling that specific type of change object.
     * @param visitor the concrete visitor to work on that object.
     */
    @Override
    public void accept(CfChangeVisitor visitor) {
        visitor.visitRemovedObject(this);
    }

}
