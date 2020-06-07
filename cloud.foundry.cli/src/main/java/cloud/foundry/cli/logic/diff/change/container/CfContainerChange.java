package cloud.foundry.cli.logic.diff.change.container;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data object that holds container changes.
 */
public class CfContainerChange extends CfChange {

    private String propertyName;
    private List<CfContainerChangeValue> changedValues;

    public String getPropertyName() {
        return propertyName;
    }

    public List<CfContainerChangeValue> getChangedValues() {
        return Collections.unmodifiableList(changedValues);
    }

    public CfContainerChange(Object affectedObject, String propertyName, List<CfContainerChangeValue> changedValues) {
        super(affectedObject);
        this.propertyName = propertyName;
        this.changedValues = new LinkedList<>(changedValues);
    }

    public List<CfContainerChangeValue> getValueChangesBy(ChangeType changeType) {
        return changedValues
                .stream()
                .filter( valueChange -> valueChange.getChangeType().equals(changeType))
                .collect(Collectors.toList());
    }

}
