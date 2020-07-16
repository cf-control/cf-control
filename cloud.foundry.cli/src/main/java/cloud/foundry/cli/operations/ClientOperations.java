package cloud.foundry.cli.operations;

import reactor.core.publisher.Mono;

/**
 * Serves has the pattern for the client operations classes
 */
public interface ClientOperations {

    Mono<String> determineApiVersion();

}
