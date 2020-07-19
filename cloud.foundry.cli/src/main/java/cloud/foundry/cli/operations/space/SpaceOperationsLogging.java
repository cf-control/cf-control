package cloud.foundry.cli.operations.space;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.SpaceOperations;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

public class SpaceOperationsLogging implements SpaceOperations {

    private static final Log log = Log.getLog(ApplicationsOperations.class);

    public SpaceOperations spaceOperations;

    public SpaceOperationsLogging(@Nonnull SpaceOperations spaceOperations) {
        this.spaceOperations = spaceOperations;
    }

    @Override
    public Mono<List<String>> getAll() {
        return this.spaceOperations.getAll()
                .doOnSubscribe(subscription -> log.debug("Querying all spaces"))
                .doOnSuccess(stringApplicationBeanMap -> log.debug("Querying all spaces finished"));
    }

    @Override
    public Mono<Void> create(String spaceName) {
        return this.spaceOperations.create(spaceName)
                .doOnSubscribe(aVoid -> log.debug("Creating space", spaceName))
                .doOnSuccess(aVoid -> log.verbose("Creating space", spaceName, "completed"));
    }
}
