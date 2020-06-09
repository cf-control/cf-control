package cloud.foundry.cli.logic.diff.change.container;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Data object that holds container changes.
 */
public class CfContainerChange extends CfChange {

    private final List<CfContainerValueChanged> changedValues;

    public List<CfContainerValueChanged> getChangedValues() {
        return Collections.unmodifiableList(changedValues);
    }

    public CfContainerChange(Object affectedObject,
                             String propertyName,
                             List<String> path,
                             List<CfContainerValueChanged> changedValues) {
        super(affectedObject, propertyName, path);
        checkNotNull(changedValues);

        this.changedValues = new LinkedList<>(changedValues);
    }

    public List<CfContainerValueChanged> getValueChangesBy(ChangeType changeType) {
        checkNotNull(changeType);

        return changedValues
                .stream()
                .filter( valueChange -> valueChange.getChangeType().equals(changeType))
                .collect(Collectors.toList());
    }

}
