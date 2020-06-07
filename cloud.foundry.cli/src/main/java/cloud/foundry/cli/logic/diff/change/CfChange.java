package cloud.foundry.cli.logic.diff.change;

/**
 * Base class for all change classes
 */
public abstract class CfChange {

    Object affectedObject;

    public Object getAffectedObject() {
        return affectedObject;
    }

    public CfChange(Object affectedObject) {
        this.affectedObject = affectedObject;
    }

}
