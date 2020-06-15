package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.List;

/**
 * This class is responsible to apply in the context of applications according to the CfChanges.
 * The class performs the apply task by implementing the {@link CfChangeVisitor} interface.
 */
public class ApplicationApplier implements CfChangeVisitor {

    private final DefaultCloudFoundryOperations cfOperations;
    private final String applicationName;

    private ApplicationApplier(DefaultCloudFoundryOperations cfOperations, String applicationName) {
        this.cfOperations = cfOperations;
        this.applicationName = applicationName;
    }

    /**
     * Apply logic for CfNewObject
     * @param newObject the CfNewObject to be visited
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        Object affectedObject = newObject.getAffectedObject();
        if (affectedObject instanceof ApplicationBean) {
            try {
                doCreateNewApp((ApplicationBean) affectedObject);
            } catch (CreationException e) {
                throw new ApplyException(e);
            }
        }
        else if (!(affectedObject instanceof ApplicationManifestBean)) {
            throw new IllegalArgumentException("Only changes of applications and manifests are permitted.");
        }
        return;
    }

    private void doCreateNewApp(ApplicationBean affectedObject) throws CreationException {
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        applicationsOperations.create(this.applicationName, affectedObject, false);
        Log.info("App created:", applicationName);
    }

    /**
     * Apply logic for CfObjectValueChanged
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        //TODO: later US
        return;
    }

    /**
     * Apply logic for CfRemovedObject
     * @param removedObject the CfRemovedObject to be visited
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        //TODO: later US
        return;
    }

    /**
     * Apply logic for CfContainerChange
     * @param containerChange the CfContainerChange to be visited
     */
    @Override
    public void visitContainerChange(CfContainerChange containerChange) {
        //TODO: later US
    }

    /**
     * Apply logic for CfMapChange
     * @param mapChange the CfMapChange to be visited
     */
    @Override
    public void visitMapChange(CfMapChange mapChange) {
        //TODO: later US
    }

    /**
     * Apply for all changes regarding one application.
     * @param cfOperations the cfOperations object connected to the cfInstance
     * @param applicationName the name of the application
     * @param applicationChanges a list with all the Changes found during diff for that specific application
     * @throws ApplyException if an error during the apply logic occurs. May contain another exception inside,
     * with more details.
     */
    public static void apply(DefaultCloudFoundryOperations cfOperations, String applicationName,
                             List<CfChange> applicationChanges) {
        ApplicationApplier applicationApplier = new ApplicationApplier(cfOperations, applicationName);
        for (CfChange applicationChange : applicationChanges) {
            applicationChange.accept(applicationApplier);
        }
    }
}
