package cloud.foundry.cli.logic.diff.change.object;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.List;


/**
 * Data object that holds a change on a scalar field value (e.g. ConfigBean.apiVersion).
 */
public class CfObjectValueChanged extends CfChange {

    private String valueBefore;
    private String valueAfter;

    /**
     * @param affectedObject the object that holds the changed scalar as a field
     * @param propertyName the field name of the scalar
     * @param path the field names of the object graph that lead to the scalar (with the compared object as root)
     * @param valueBefore the value before the change
     * @param valueAfter the value after the change
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the path does not contain a root (i.e. if the path is empty)
     */
    public CfObjectValueChanged(Object affectedObject,
                                String propertyName,
                                List<String> path,
                                String valueBefore,
                                String valueAfter) {
        super(affectedObject, propertyName, path);
        checkNotNull(valueBefore);
        checkNotNull(valueAfter);

        this.valueBefore = valueBefore;
        this.valueAfter = valueAfter;
    }

    public String getValueBefore() {
        return valueBefore;
    }

    public String getValueAfter() {
        return valueAfter;
    }
}
