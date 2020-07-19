package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Serves has the pattern for the service operations classes
 */
public interface ServicesOperations {

    Mono<Map<String, ServiceBean>> getAll();

    Mono<Void> create(String serviceInstanceName, ServiceBean serviceBean);

    Mono<Void> rename(String newName, String currentName);

    Mono<Void> update(String serviceInstanceName, ServiceBean serviceBean);

    Mono<Void> remove(String serviceInstanceName);

    Flux<Void> deleteKeys(String serviceInstanceName);

    Flux<Void> unbindApps(String serviceInstanceName);

    Flux<Void> unbindRoutes(String serviceInstanceName);

}
