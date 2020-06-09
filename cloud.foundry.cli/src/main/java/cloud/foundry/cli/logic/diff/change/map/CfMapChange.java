package cloud.foundry.cli.logic.diff.change.map;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Data object that holds Map changes.
 */
public class CfMapChange extends CfChange {

    private final List<CfMapValueChanged> changedValues;

    public List<CfMapValueChanged> getChangedValues() {
        return Collections.unmodifiableList(changedValues);
    }

    public CfMapChange(Object affectedObject,
                       String propertyName,
                       List<String> path,
                       List<CfMapValueChanged> changedValues) {
        super(affectedObject, propertyName, path);
        checkNotNull(changedValues);

        this.changedValues = new LinkedList<>(changedValues);
    }

    public List<CfMapValueChanged> getValueChangesBy(ChangeType changeType) {
        return changedValues
                .stream()
                .filter( valueChange -> valueChange.getChangeType().equals(changeType))
                .collect(Collectors.toList());
    }
}
