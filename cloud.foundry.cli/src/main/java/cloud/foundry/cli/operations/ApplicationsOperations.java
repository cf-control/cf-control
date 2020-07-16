package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Serves has the pattern for the application operations classes
 */
public interface ApplicationsOperations {

    Mono<Map<String, ApplicationBean>> getAll();

    Mono<Void> remove(String applicationName);

    Mono<Void> update(String appName, ApplicationBean bean, boolean shouldStart);

    Mono<Void> create(String appName, ApplicationBean bean, boolean shouldStart);

    Mono<Void> rename(String newName, String currentName);

    Mono<Void> scale(String applicationName, Integer diskLimit, Integer memoryLimit, Integer instances);

    Mono<Void> addEnvironmentVariable(String applicationName, String variableName, String variableValue);

    Mono<Void> removeEnvironmentVariable(String applicationName, String variableName);

    Mono<Void> setHealthCheck(String applicationName, ApplicationHealthCheck healthCheckType);

    Mono<Void> bindToService(String applicationName, String serviceName);

    Mono<Void> unbindFromService(String applicationName, String serviceName);

    Mono<Void> addRoute(String applicationName, String route);

    Mono<Void> removeRoute(String applicationName, String route);

}
