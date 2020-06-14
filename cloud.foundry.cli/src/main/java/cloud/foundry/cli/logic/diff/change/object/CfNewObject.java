package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.crosscutting.exceptions.ApplyExcpetion;
import cloud.foundry.cli.logic.apply.ApplyVisitor;
import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.List;

/**
 * Holds the information that an object was added.
 */
public class CfNewObject extends CfChange {

    /**
     * @param affectedObject the object that holds the new object as a field
     * @param propertyName the field name of the new object
     * @param path the field names of the object graph that lead to new object (with the compared object as root)
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the path does not contain a root (i.e. if the path is empty)
     */
    public CfNewObject(Object affectedObject, String propertyName, List<String> path) {
        super(affectedObject, propertyName, path);
    }

    /**
     * Call the visitNewObject method from the visitor.
     * @param visitor
     */
    @Override
    public void accept(ApplyVisitor visitor) throws ApplyExcpetion {
        visitor.visitNewObject(this);
    }
}
