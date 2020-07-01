package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import reactor.core.publisher.Flux;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * This class is responsible to build the requests in the context of
 * applications according to the CfChanges. The class does create the request
 * tasks by implementing the {@link CfChangeVisitor} interface.
 */
public class ApplicationRequestsPlaner extends RequestsPlaner {

    private static final Log log = Log.getLog(ApplicationRequestsPlaner.class);

    private final ApplicationsOperations appOperations;
    private final String applicationName;

    private ApplicationRequestsPlaner(ApplicationsOperations appOperations, String applicationName) {
        this.appOperations = appOperations;
        this.applicationName = applicationName;
    }

    /**
     * Creates the request for CfNewObject
     *
     * @param newObject the CfNewObject to be visited
     * @throws NullPointerException     if the argument is null
     * @throws IllegalArgumentException if the newObject is neither an
     *                                  ApplicationBean or an ApplicationManifestBean
     * @throws ApplyException           that wraps the exceptions that can occur
     *                                  during the creation procedure.
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        checkNotNull(newObject);

        Object affectedObject = newObject.getAffectedObject();
        if (affectedObject instanceof ApplicationBean) {
            try {
                addCreateAppRequest((ApplicationBean) affectedObject);
            } catch (CreationException | IllegalArgumentException | NullPointerException | SecurityException e) {
                throw new ApplyException(e);
            }
        } else if (!(affectedObject instanceof ApplicationManifestBean)) {
            throw new IllegalArgumentException("Only changes of applications and manifests are permitted.");
        }
    }

    private void addCreateAppRequest(ApplicationBean affectedObject) throws CreationException {
        this.addRequest(this.appOperations.create(this.applicationName, affectedObject, false));
    }

    /**
     * Creates the requests for CfRemovedObject
     *
     * @param removedObject the CfRemovedObject to be visited
     * @throws NullPointerException     if the argument is null
     * @throws IllegalArgumentException if the newObject is neither an
     *                                  ApplicationBean or an ApplicationManifestBean
     */
    @Override
    public void visitRemovedObject(@Nonnull CfRemovedObject removedObject) {
        checkNotNull(removedObject);

        Object affectedObject = removedObject.getAffectedObject();
        if (affectedObject instanceof ApplicationBean) {
            addRemoveAppRequest();
        } else if (!(affectedObject instanceof ApplicationManifestBean)) {
            throw new IllegalArgumentException("Only changes of applications and manifests are permitted.");
        }
    }

    private void addRemoveAppRequest() {
        this.addRequest(this.appOperations.remove(this.applicationName));
    }

    /**
     * Creates the remove/create requests for one application.
     *
     * @param appOperations      the ApplicationOperations object used for
     * @param applicationName    the name of the application
     * @param applicationChanges a list with all the Changes found during diff for
     *                           that specific application
     * @throws IllegalArgumentException if the newObject is neither an ApplicationBean or an ApplicationManifestBean
     * @throws NullPointerException if any of the arguments are null
     * @return flux of all requests that are required to apply the changes
     */
    public static Flux<Void> createApplyRequests(@Nonnull ApplicationsOperations appOperations,
        @Nonnull String applicationName,
        @Nonnull List<CfChange> applicationChanges) {
        checkNotNull(appOperations);
        checkNotNull(applicationName);
        checkNotNull(applicationChanges);

        ApplicationRequestsPlaner applicationRequestsPlaner = new ApplicationRequestsPlaner(appOperations,
            applicationName);

        for (CfChange applicationChange : applicationChanges) {
            applicationChange.accept(applicationRequestsPlaner);
        }

        return Flux.merge(applicationRequestsPlaner.getRequests());
    }
}
