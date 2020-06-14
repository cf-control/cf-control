package cloud.foundry.cli.logic.apply;


import cloud.foundry.cli.crosscutting.exceptions.ApplyExcpetion;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;


/**
 * This provides a visitor interface for the apply logic.
 */
public interface ApplyVisitor {

    void visitNewObject(CfNewObject newObject) throws ApplyExcpetion;

    void visitObjectValueChanged(CfObjectValueChanged objectValueChanged);

    void visitRemovedObject(CfRemovedObject removedObject);

}
