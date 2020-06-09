package cloud.foundry.cli.logic.diff.change.object;

import cloud.foundry.cli.logic.diff.change.CfChange;

import java.util.List;

public class CfNewObject extends CfChange {

    public CfNewObject(Object affectedObject, String propertyName, List<String> path) {
        super(affectedObject, propertyName, path);
    }
}
