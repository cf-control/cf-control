package cloud.foundry.cli.operations;

import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Serves has the pattern for the space developers operations classes
 */
public interface SpaceDevelopersOperations {

    Mono<List<String>> getAll();

    Mono<String> getSpaceId();

    Mono<Void> assign(@Nonnull String username, @Nonnull String spaceId);

    Mono<Void> remove(String username, String spaceId);

}
