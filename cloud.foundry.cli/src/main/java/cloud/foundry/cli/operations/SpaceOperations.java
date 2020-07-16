package cloud.foundry.cli.operations;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Serves has the pattern for the space operations classes
 */
public interface SpaceOperations {

    Mono<List<String>> getAll();

    Mono<Void> create(String spaceName);

}
