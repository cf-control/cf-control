package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.crosscutting.exceptions.ApplyExcpetion;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;


/**
 * This provides a visitor interface for the apply logic.
 */
public interface CfChangeVisitor {

    void visitNewObject(CfNewObject newObject) throws ApplyExcpetion;

    void visitObjectValueChanged(CfObjectValueChanged objectValueChanged);

    void visitRemovedObject(CfRemovedObject removedObject);

    void visitContainerChange(CfContainerChange containerChange);

    void visitMapChange(CfMapChange mapChange);


}
