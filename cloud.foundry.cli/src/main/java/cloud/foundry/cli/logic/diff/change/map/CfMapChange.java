package cloud.foundry.cli.logic.diff.change.map;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data object that holds Map changes.
 */
public class CfMapChange extends CfChange {

    private String propertyName;
    private List<CfMapChangeValue> changedValues;

    public String getPropertyName() {
        return propertyName;
    }

    public List<CfMapChangeValue> getChangedValues() {
        return Collections.unmodifiableList(changedValues);
    }

    public CfMapChange(Object affectedObject , String propertyName, List<CfMapChangeValue> changedValues) {
        super(affectedObject);
        this.propertyName = propertyName;
        this.changedValues = new LinkedList<>(changedValues);
    }

    public List<CfMapChangeValue> getValueChangesBy(ChangeType changeType) {
        return changedValues
                .stream()
                .filter( valueChange -> valueChange.getChangeType().equals(changeType))
                .collect(Collectors.toList());
    }
}
