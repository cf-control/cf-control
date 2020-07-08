package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * This is the super class of all request planer classes which are responsible to build the required requests
 * for their operation domain according to the given CfChange objects.
 * The class does create the request tasks by implementing the {@link CfChangeVisitor} interface.
 */
public abstract class RequestsPlaner implements CfChangeVisitor {

    protected enum RequestType {
        NONE,
        CREATE,
        REMOVE,
        CHANGE_INPLACE,
        CHANGE_RESTART
    }

    /**
     * error message when the change type is not supported
     */
    private static final String CHANGE_TYPE_IS_NOT_SUPPORTED = "Change type is not supported.";

    /**
     * the type of request that is necessary to apply the needed changes
     */
    protected RequestType requestType;

    private final List<Mono<Void>> requests;

    protected RequestsPlaner() {
        this.requests = new LinkedList<>();
        this.requestType = RequestType.NONE;
    }

    /**
     * @return an unmodifiable list of the requests
     */
    protected List<Mono<Void>> getRequests() {
        return Collections.unmodifiableList(this.requests);
    }

    /**
     * adds a request for the current domain object (app, service etc...) which should be applied to the cf instance
     * @param request the request which should be added
     * @throws NullPointerException when the argument was null
     */
    protected void addRequest(Mono<Void> request) {
        checkNotNull(request);

        this.requests.add(request);
    }

    /**
     * Creates the request CfNewObject
     * @param newObject the CfNewObject to be visited
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        throw new ApplyException(CHANGE_TYPE_IS_NOT_SUPPORTED);
    }

    /**
     * Creates the requests CfContainerChange
     * @param containerChange the CfContainerChange to be visited
     */
    @Override
    public void visitContainerChange(CfContainerChange containerChange) {
        throw new ApplyException(CHANGE_TYPE_IS_NOT_SUPPORTED);
    }

    /**
     * Creates the requests for CfMapChange
     * @param mapChange the CfMapChange to be visited
     */
    @Override
    public void visitMapChange(CfMapChange mapChange) {
        throw new ApplyException(CHANGE_TYPE_IS_NOT_SUPPORTED);
    }

    /**
     * Creates the requests for CfObjectValueChanged
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        throw new ApplyException(CHANGE_TYPE_IS_NOT_SUPPORTED);
    }

    /**
     * Creates the requests for CfRemovedObject
     * @param removedObject the CfRemovedObject to be visited
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        throw new ApplyException(CHANGE_TYPE_IS_NOT_SUPPORTED);
    }


}
