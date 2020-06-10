package cloud.foundry.cli.logic.diff.change.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.ChangeType;

/**
 * Data object that holds a container value change.
 * These changes can be either ChangeType.ADDED or ChangeType.REMOVED.
 * Since we are storing container types only in roots (e.g. spaceDevelopers), there will be no ChangeType.CHANGED.
 */
public class CfContainerValueChanged {

    private String value;
    private ChangeType changeType;

    /**
     * @return the value was either removed or added, depending on the change type
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the change type which can either be ChangeType.ADDED or ChangeType.REMOVED
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * @param value the value that changed
     * @param changeType whether the value was added or removed
     * @throws IllegalArgumentException if the change type is not added or removed (i.e. changed)
     */
    public CfContainerValueChanged(String value, ChangeType changeType) {
        checkNotNull(value);
        checkNotNull(changeType);
        checkArgument(changeType != ChangeType.CHANGED,
                "The change type must either be added or removed");

        this.value = value;
        this.changeType = changeType;
    }
}
