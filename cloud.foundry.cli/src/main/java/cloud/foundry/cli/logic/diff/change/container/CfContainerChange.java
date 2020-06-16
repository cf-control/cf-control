package cloud.foundry.cli.logic.diff.change.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.apply.CfChangeVisitor;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data object that holds changes that occurred in a container.
 */
public class CfContainerChange extends CfChange {

    private final List<CfContainerValueChanged> changedValues;

    /**
     * @param affectedObject the object that holds the changed container as a field
     * @param propertyName the field name of the container
     * @param path the field names of the object graph that lead to the container (with the compared object as root)
     * @param changedValues the changes within the container
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the path does not contain a root (i.e. if the path is empty)
     *                                  or if the changed values are empty
     */
    public CfContainerChange(Object affectedObject,
                             String propertyName,
                             List<String> path,
                             List<CfContainerValueChanged> changedValues) {
        super(affectedObject, propertyName, path);
        checkNotNull(changedValues);
        checkArgument(!changedValues.isEmpty(), "The changed values cannot be empty.");

        this.changedValues = new LinkedList<>(changedValues);
    }

    /**
     * @return all changed values of the container as unmodifiable list
     */
    public List<CfContainerValueChanged> getChangedValues() {
        return Collections.unmodifiableList(changedValues);
    }

    /**
     * @param changeType the desired type of changes
     * @return a list of container changes containing all changes with the desired change type
     * @throws NullPointerException if the change type is null
     */
    public List<CfContainerValueChanged> getValueChangesBy(ChangeType changeType) {
        checkNotNull(changeType);

        return changedValues
                .stream()
                .filter( valueChange -> valueChange.getChangeType().equals(changeType))
                .collect(Collectors.toList());
    }

    /**
     * Accept a visitor handling that specific type of change object.
     * @param visitor the concrete visitor to work on that object.
     */
    @Override
    public void accept(CfChangeVisitor visitor) {
        visitor.visitContainerChange(this);
    }
}
