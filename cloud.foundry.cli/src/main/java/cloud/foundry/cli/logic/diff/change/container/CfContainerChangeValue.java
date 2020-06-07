package cloud.foundry.cli.logic.diff.change.container;

import cloud.foundry.cli.logic.diff.change.ChangeType;

/**
 * Data object that holds a container value change.
 * These changes can be ChangeType.REMOVED or ChangeType.DELETED.
 * Since we are storing container types only in roots (e.g. spaceDevelopers), there will be no ChangeType.CHANGED.
 */
public class CfContainerChangeValue {

    private String value;
    private ChangeType changeType;

    public String getValue() {
        return value;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public CfContainerChangeValue(String value, ChangeType changeType) {
        assert changeType != ChangeType.CHANGED;

        this.value = value;
        this.changeType = changeType;
    }
}
