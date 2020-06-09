package cloud.foundry.cli.logic.diff.change.map;

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

    public CfMapValueChanged(String key, String valueBefore, String valueAfter, ChangeType changeType) {
        this.key = key;
        this.valueBefore = valueBefore;
        this.valueAfter = valueAfter;
        this.changeType = changeType;
    }
}
