package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;

import java.util.List;

public class ApplyResolver implements ApplyVisitor {

    @Override
    public void visitNewObject(CfNewObject newObject) {
        //TODO: this US
        return;
    }

    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        //TODO: later US
        return;
    }

    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        //TODO: later US
        return;
    }

    public void applyOnAppChanges(List<CfChange> applicationChanges) {
        for (CfChange applicationChange : applicationChanges) {
            applicationChange.accept(this);
        }
    }
}
