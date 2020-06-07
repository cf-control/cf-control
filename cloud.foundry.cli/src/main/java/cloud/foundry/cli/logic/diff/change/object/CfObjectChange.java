package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

/**
 * Data object that holds object changes.
 * It's either a new object or a removed object.
 */
public class CfObjectChange extends CfChange {

    ChangeType changeType;

    public ChangeType getChangeType() {
        return changeType;
    }

    public CfObjectChange(Object affectedObject, ChangeType changeType) {
        super(affectedObject);
        assert changeType != ChangeType.CHANGED;

        this.changeType = changeType;
    }

}
