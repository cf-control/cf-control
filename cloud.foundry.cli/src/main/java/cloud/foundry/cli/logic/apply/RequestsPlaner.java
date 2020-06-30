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
 * This is the super class of all request planing classes which are responsible to build the required requests
 * according to the given CfChange objects.
 * The class does create the request tasks by implementing the {@link CfChangeVisitor} interface.
 */
public abstract class RequestsPlaner implements CfChangeVisitor {

    public static final String CHANGE_TYPE_IS_NOT_SUPPORTED = "Change type is not supported.";

    private final List<Mono<Void>> requests;

    protected List<Mono<Void>> getRequests() {
        return Collections.unmodifiableList(this.requests);
    }

    protected RequestsPlaner() {
        this.requests = new LinkedList<>();
    }

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