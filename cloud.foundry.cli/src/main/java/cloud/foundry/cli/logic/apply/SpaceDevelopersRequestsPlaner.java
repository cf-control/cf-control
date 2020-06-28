package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible to build the requests in the context of applications according to the CfChanges.
 * The class does creates the request tasks by implementing the {@link CfChangeVisitor} interface.
 */
public class SpaceDevelopersRequestsPlaner implements CfChangeVisitor {

    private static final Log log = Log.getLog(SpaceDevelopersRequestsPlaner.class);

    private final SpaceDevelopersOperations spaceDevelopersOperations;
    private final List<Mono<Void>> requests;

    private SpaceDevelopersRequestsPlaner(SpaceDevelopersOperations spaceDevelopersOperations) {
        this.spaceDevelopersOperations = spaceDevelopersOperations;
        this.requests = new LinkedList<>();
    }

    /**
     * Creates the request for CfNewObject
     *
     * @param newObject the CfNewObject to be visited
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
    }

    /**
     * Creates the requests for CfObjectValueChanged
     *
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        return;
    }

    /**
     * Creates the requests for CfRemovedObject
     *
     * @param removedObject the CfRemovedObject to be visited
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        return;
    }

    /**
     * Creates the requests CfContainerChange
     *
     * @param containerChange the CfContainerChange to be visited
     */
    @Override
    public void visitContainerChange(CfContainerChange containerChange) {
        String spaceId = spaceDevelopersOperations.getSpaceId().block();
        containerChange.getChangedValues().forEach(
                changedValue -> {
                    if (changedValue.getChangeType() == ChangeType.ADDED) {
                        addSpaceDeveloperRequest(changedValue.getValue(), spaceId);
                    } else if (changedValue.getChangeType() == ChangeType.REMOVED) {
                        removeSpaceDeveloperRequest(changedValue.getValue(), spaceId);
                    }
                }
        );
    }

    /**
     * Creates the requests for CfMapChange
     *
     * @param mapChange the CfMapChange to be visited
     */
    @Override
    public void visitMapChange(CfMapChange mapChange) {
        return;
    }

    public static Flux<Void> applySpaceDevelopers(
            SpaceDevelopersOperations spaceDevelopersOperations,
            List<CfChange> spaceDevelopersChanges) {

        SpaceDevelopersRequestsPlaner spaceDevelopersRequestsPlaner =
                new SpaceDevelopersRequestsPlaner(spaceDevelopersOperations);

        for (CfChange spaceDeveloperChange : spaceDevelopersChanges) {
            spaceDeveloperChange.accept(spaceDevelopersRequestsPlaner);
        }

        return Flux.merge(spaceDevelopersRequestsPlaner.requests);
    }

    private void addSpaceDeveloperRequest(String username, String spaceId) throws CreationException {
        this.requests.add(this.spaceDevelopersOperations.assign(username, spaceId));
    }

    private void removeSpaceDeveloperRequest(String username, String spaceId) throws CreationException {
        this.requests.add(this.spaceDevelopersOperations.remove(username, spaceId));
    }

}
