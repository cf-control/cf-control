package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.logic.diff.change.CfChange;

/**
 * Data object that holds a change on a scalar field value (e.g. ConfigBean.apiVersion).
 */
public class CfObjectValueChange extends CfChange {

    private String propertyName;
    private String valueBefore;
    private String valueAfter;

    public String getPropertyName() {
        return propertyName;
    }

    public String getValueBefore() {
        return valueBefore;
    }

    public String getValueAfter() {
        return valueAfter;
    }

    public CfObjectValueChange(Object affectedObject, String valueBefore, String valueAfter, String propertyName) {
        super(affectedObject);
        this.valueBefore = valueBefore;
        this.valueAfter = valueAfter;
        this.propertyName = propertyName;
    }
}
