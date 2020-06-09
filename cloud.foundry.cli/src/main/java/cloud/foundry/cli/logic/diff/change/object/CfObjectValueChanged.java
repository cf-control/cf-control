package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Data object that holds a change on a scalar field value (e.g. ConfigBean.apiVersion).
 */
public class CfObjectValueChanged extends CfChange {

    private String valueBefore;
    private String valueAfter;
    private ChangeType changeType;

    public String getValueBefore() {
        return valueBefore;
    }

    public String getValueAfter() {
        return valueAfter;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public CfObjectValueChanged(Object affectedObject,
                                String propertyName,
                                List<String> path,
                                ChangeType changeType,
                                String valueBefore,
                                String valueAfter) {
        super(affectedObject, propertyName, path);
        checkNotNull(changeType);
        checkNotNull(valueBefore);
        checkNotNull(valueAfter);

        this.changeType = changeType;
        this.valueBefore = valueBefore;
        this.valueAfter = valueAfter;
    }
}
