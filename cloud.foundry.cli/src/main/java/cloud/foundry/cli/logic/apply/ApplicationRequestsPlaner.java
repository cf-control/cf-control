package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.*;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * This class is responsible to build the requests in the context of
 * applications according to the CfChanges. The class does create the request
 * tasks by implementing the {@link CfChangeVisitor} interface.
 */
public class ApplicationRequestsPlaner extends RequestsPlaner {

    private static final Log log = Log.getLog(ApplicationRequestsPlaner.class);
    private static final Set<String> FIELDS_REQUIRE_RESTART = new HashSet<>(Arrays.asList("meta", "path"));

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
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        checkNotNull(newObject);
        checkArgument(newObject.getAffectedObject() instanceof ApplicationBean,
                "Change object must contain an ApplicationBean");
        checkState(this.requestType == RequestType.NONE,
                "Trying to process new object when app will be removed or changed already.");

        this.requestType = RequestType.CREATE;
    }

    /**
     * Creates the requests for CfObjectValueChanged
     *
     * @param objectValueChanged the CfObjectValueChanged to be visited
     * @throws NullPointerException     if the argument is null
     * @throws IllegalArgumentException if the newObject is neither an
     *                                  ApplicationBean or an ApplicationManifestBean
     */

    @Override
    public void visitObjectValueChanged(@Nonnull CfObjectValueChanged objectValueChanged) {
        checkNotNull(objectValueChanged);
        checkArgument(objectValueChanged.getAffectedObject() instanceof ApplicationBean,
                "Change object must contain an ApplicationBean");
        checkState(this.requestType != RequestType.CREATE && this.requestType != RequestType.REMOVE,
                "Trying to process change object when app will be added or removed already.");

        // Already restarting, nothing to do
        if (this.requestType == RequestType.CHANGE_RESTART) return;

        // if the field where the change as taken place can only be changed through restarting
        if (FIELDS_REQUIRE_RESTART.contains(objectValueChanged.getPropertyName())) {
            this.requestType = RequestType.CHANGE_RESTART;
            return;
        }

        this.requestType = RequestType.CHANGE_INPLACE;
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
        checkArgument(removedObject.getAffectedObject() instanceof ApplicationBean ,
                "Change object must contain an ApplicationBean");
        checkState(requestType == RequestType.NONE,
                "Trying to process remove object when app will be added or changed already.");

        this.requestType = RequestType.REMOVE;
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

        try {
            ApplicationRequestsPlaner applicationRequestsPlaner = new ApplicationRequestsPlaner(appOperations,
                    applicationName);

            return applicationRequestsPlaner.doCreateApplyRequests(applicationChanges);
        } catch (Exception exception) {
            throw new ApplyException(exception);
        }
    }

    private Flux<Void> doCreateApplyRequests(List<CfChange> applicationChanges) {
        return applicationChanges
                .stream()
                .peek(change -> change.accept(this))
                .findFirst()
                .map(change -> this.determineRequest((ApplicationBean) change.getAffectedObject()))
                .orElse(Flux.empty());

    }

    private Flux<Void> determineRequest(ApplicationBean applicationBean) {
        switch (this.requestType) {
            case CREATE:
                log.debug("Add create request for app: " + applicationName);
                return Flux.merge(this.appOperations.create(this.applicationName, applicationBean, false));
            case REMOVE:
                log.debug("Add remove request for app: " + applicationName);
                return Flux.merge(this.appOperations.remove(this.applicationName));
            case CHANGE_RESTART:
                log.debug("Add update with restart request for app: " + applicationName);
                return Flux.merge(this.appOperations.create(this.applicationName, applicationBean, false));
            case CHANGE_INPLACE:
                System.out.println("UPDATING APP INPLACE: " + applicationName);
                // TODO scale, env vars or healthcheck type
            default:
                return Flux.empty();
        }
    }
}
