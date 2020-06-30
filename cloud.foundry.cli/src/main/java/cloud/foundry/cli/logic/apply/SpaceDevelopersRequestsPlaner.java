package cloud.foundry.cli.logic.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible to build the requests in the context of space developers according to the CfChanges.
 */
public class SpaceDevelopersRequestsPlaner {

    private static final Log log = Log.getLog(SpaceDevelopersRequestsPlaner.class);

    private final SpaceDevelopersOperations spaceDevelopersOperations;
    private final List<Mono<Void>> requests;

    public SpaceDevelopersRequestsPlaner(SpaceDevelopersOperations spaceDevelopersOperations) {
        this.spaceDevelopersOperations = spaceDevelopersOperations;
        this.requests = new LinkedList<>();
    }

    /**
     * Creates the requests to assign/revoke space developer's permission.
     *
     * @param spaceDevelopersOperations the SpaceDevelopersOperations object.
     * @param spaceDevelopersChange a list with all the Changes found during diff for the specific space developers.
     * @return Flux of all requests that are required to apply the changes.
     * @throws NullPointerException if one of the argument is null.
     */
    public Flux<Void> createSpaceDevelopersRequests(SpaceDevelopersOperations spaceDevelopersOperations,
                                                           CfContainerChange spaceDevelopersChange) {

        checkNotNull(spaceDevelopersOperations);
        checkNotNull(spaceDevelopersChange);

        String spaceId = spaceDevelopersOperations.getSpaceId().block();
        spaceDevelopersChange.getChangedValues().forEach(
                changedValue -> {
                    if (changedValue.getChangeType() == ChangeType.ADDED) {
                        addSpaceDeveloperRequest(changedValue.getValue(), spaceId);
                    } else if (changedValue.getChangeType() == ChangeType.REMOVED) {
                        removeSpaceDeveloperRequest(changedValue.getValue(), spaceId);
                    }
                }
        );

        return Flux.merge(this.requests);
    }

    /**
     * Creates a request for assigning a space developer to the cf instance.
     *
     * @param username email of user to assign as space developer.
     * @param spaceId the id of the space.
     * @throws NullPointerException if any of the arguments are null.
     */
    private void addSpaceDeveloperRequest(String username, String spaceId) {
        this.requests.add(this.spaceDevelopersOperations.assign(username, spaceId));
    }

    /**
     * Creates a request for removing a space developer of the cf instance.
     *
     * @param username email of user to remove as space developer.
     * @param spaceId the id of the space.
     * @throws NullPointerException if any of the arguments are null.
     */
    private void removeSpaceDeveloperRequest(String username, String spaceId) {
        this.requests.add(this.spaceDevelopersOperations.remove(username, spaceId));
    }

}
