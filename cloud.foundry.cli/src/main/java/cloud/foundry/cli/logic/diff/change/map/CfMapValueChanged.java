package cloud.foundry.cli.logic.diff.change.map;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.ChangeType;

/**
 * Data object that holds a map value change.
 * These changes can be ChangeType.REMOVED, ChangeType.DELETED, ChangeType.CHANGED.
 */
public class CfMapValueChanged {

    private String key;
    private String valueBefore;
    private String valueAfter;
    private ChangeType changeType;

    public String getKey() { return key; }

    public String getValueBefore() {
        return valueBefore;
    }

    public String getValueAfter() { return valueAfter; }

    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * @param key the key of which the value changes
     * @param valueBefore the value before the change
     * @param valueAfter the value after the change
     * @param changeType whether the value was added, removed or changed
     */
    public CfMapValueChanged(String key, String valueBefore, String valueAfter, ChangeType changeType) {
        checkNotNull(key);
        checkNotNull(valueBefore);
        checkNotNull(valueAfter);
        checkNotNull(changeType);

        this.key = key;
        this.valueBefore = valueBefore;
        this.valueAfter = valueAfter;
        this.changeType = changeType;
    }
}
