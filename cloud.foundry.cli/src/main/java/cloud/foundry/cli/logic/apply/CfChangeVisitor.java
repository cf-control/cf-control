package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;


/**
 * This is a visitor interface used for handling CfChanges.
 */
public interface CfChangeVisitor {

    /**
     * This method is called when the visitor visits a CfNewObject.
     * @param newObject the CfNewObject to be visited
     */
    void visitNewObject(CfNewObject newObject);

    /**
     * This method is called when the visitor visits a CfObjectValueChanged.
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    void visitObjectValueChanged(CfObjectValueChanged objectValueChanged);

    /**
     * This method is called when the visitor visits a CfRemovedObject.
     * @param removedObject the CfRemovedObject to be visited
     */
    void visitRemovedObject(CfRemovedObject removedObject);

    /**
     * This method is called when the visitor visits a CfContainerChange.
     * @param containerChange the CfContainerChange to be visited
     */
    void visitContainerChange(CfContainerChange containerChange);

    /**
     * This method is called when the visitor visits a CfMapChange.
     * @param mapChange the CfMapChange to be visited
     */
    void visitMapChange(CfMapChange mapChange);


}
