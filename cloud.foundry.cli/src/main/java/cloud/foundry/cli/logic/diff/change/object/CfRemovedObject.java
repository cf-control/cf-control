package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.List;

/**
 * Data object that holds object changes.
 * It's either a new object or a removed object.
 */
public class CfRemovedObject extends CfChange {

    public CfRemovedObject(Object affectedObject, String propertyName, List<String> path) {
        super(affectedObject, propertyName, path);
    }

}
