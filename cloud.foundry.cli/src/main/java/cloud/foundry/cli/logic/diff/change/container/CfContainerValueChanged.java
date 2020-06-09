package cloud.foundry.cli.logic.diff.change.container;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.ChangeType;

/**
 * Data object that holds a container value change.
 * These changes can be ChangeType.REMOVED or ChangeType.DELETED.
 * Since we are storing container types only in roots (e.g. spaceDevelopers), there will be no ChangeType.CHANGED.
 */
public class CfContainerValueChanged {

    private String value;
    private ChangeType changeType;

    public String getValue() {
        return value;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public CfContainerValueChanged(String value, ChangeType changeType) {
        checkNotNull(value);
        checkNotNull(changeType);
        assert changeType != ChangeType.CHANGED;

        this.value = value;
        this.changeType = changeType;
    }
}
