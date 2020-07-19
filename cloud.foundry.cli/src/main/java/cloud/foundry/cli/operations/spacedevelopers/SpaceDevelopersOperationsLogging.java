package cloud.foundry.cli.operations.spacedevelopers;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

public class SpaceDevelopersOperationsLogging implements SpaceDevelopersOperations {

    private final Log log;

    private SpaceDevelopersOperations spaceDevelopersOperations;

    public SpaceDevelopersOperationsLogging(@Nonnull SpaceDevelopersOperations spaceDevelopersOperations) {
        this.spaceDevelopersOperations = spaceDevelopersOperations;
        this.log = Log.getLog(spaceDevelopersOperations.getClass());
    }

    @Override
    public Mono<List<String>> getAll() {
        return this.spaceDevelopersOperations.getAll()
                .doOnSubscribe(subscription -> log.debug("Querying all space developers"))
                .doOnSuccess(stringApplicationBeanMap -> log.debug("Querying all space developers finished"));
    }

    @Override
    public Mono<String> getSpaceId() {
        return this.spaceDevelopersOperations.getSpaceId()
                .doOnSubscribe(subscription -> log.debug("Querying for the space ID"))
                .doOnSuccess(stringApplicationBeanMap -> log.debug("Querying for the space ID completed"));

    }

    @Override
    public Mono<Void> assign(@Nonnull String username, @Nonnull String spaceId) {
        return this.spaceDevelopersOperations.assign(username, spaceId)
                .doOnSubscribe(subscription -> log.debug("Assigning space developer", username))
                .doOnSuccess(subscription -> log.verbose("Assigning space developer", username, "completed"));
    }

    @Override
    public Mono<Void> remove(String username, String spaceId) {
        return this.spaceDevelopersOperations.remove(username, spaceId)
                .doOnSubscribe(subscription -> log.debug("Removing space developer", username))
                .doOnSuccess(subscription -> log.verbose("Removing space developer", username, "completed"));
    }
}
