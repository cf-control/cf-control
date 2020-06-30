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

    /**
     * Creates the requests to assign/revoke space developer's permission.
     *
     * @param spaceDevelopersOperations the SpaceDevelopersOperations object.
     * @param spaceDevelopersChange a list with all the Changes found during diff for the specific space developers.
     * @return Flux of all requests that are required to apply the changes.
     * @throws NullPointerException if one of the argument is null.
     */
    public static Flux<Void> createSpaceDevelopersRequests(SpaceDevelopersOperations spaceDevelopersOperations,
                                                           CfContainerChange spaceDevelopersChange) {

        checkNotNull(spaceDevelopersOperations);
        checkNotNull(spaceDevelopersChange);

        String spaceId = spaceDevelopersOperations.getSpaceId().block();
        List<Mono<Void>> requests = new LinkedList<>();
        spaceDevelopersChange.getChangedValues().forEach(
                changedValue -> {
                    if (changedValue.getChangeType() == ChangeType.ADDED) {
                        requests.add(spaceDevelopersOperations.assign(changedValue.getValue(), spaceId));
                    } else if (changedValue.getChangeType() == ChangeType.REMOVED) {
                        requests.add(spaceDevelopersOperations.remove(changedValue.getValue(), spaceId));
                    }
                }
        );

        return Flux.merge(requests);
    }

}
