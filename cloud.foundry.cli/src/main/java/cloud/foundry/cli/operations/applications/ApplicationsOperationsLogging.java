package cloud.foundry.cli.operations.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * This class utilizes the decorator pattern to apply logging to the underlying applications operations
 */
public class ApplicationsOperationsLogging implements ApplicationsOperations {

    private final Log log;

    ApplicationsOperations applicationsOperations;

    public ApplicationsOperationsLogging(@Nonnull ApplicationsOperations applicationsOperations) {
        checkNotNull(applicationsOperations);

        this.applicationsOperations = applicationsOperations;
        this.log = Log.getLog(applicationsOperations.getClass());
    }


    @Override
    public Mono<Map<String, ApplicationBean>> getAll() {
        return this.applicationsOperations.getAll()
                .doOnSubscribe(subscription -> log.debug("Querying all applications"))
                .doOnSuccess(stringApplicationBeanMap -> log.debug("Querying all applications finished"));
    }

    @Override
    public Mono<Void> remove(String applicationName) {
        return this.applicationsOperations.remove(applicationName)
                .doOnSubscribe(aVoid -> log.info("Removing application", applicationName))
                .doOnSuccess(aVoid -> log.verbose("Removing application", applicationName, "completed"));
    }

    @Override
    public Mono<Void> update(String appName, ApplicationBean bean, boolean shouldStart) {
        return this.applicationsOperations.update(appName, bean, shouldStart)
                .doOnSubscribe(aVoid -> log.info("Updating application", appName))
                .doOnSuccess(aVoid -> log.info("Updating application", appName, "completed"));
    }

    @Override
    public Mono<Void> create(String appName, ApplicationBean bean, boolean shouldStart) {
        return this.applicationsOperations.create(appName, bean, shouldStart)
                .doOnSubscribe(subscription -> log.debug("Creating application", appName))
                .doOnSuccess(aVoid -> log.info("Creating application", appName, "completed"));
    }

    @Override
    public Mono<Void> rename(String newName, String currentName) {
        return this.applicationsOperations.rename(newName, currentName)
                .doOnSubscribe(aVoid -> log.info("Renaming application", currentName, "to", newName))
                .doOnSuccess(aVoid -> log.verbose("Renaming of application", currentName, "to", newName, "completed"));
    }

    @Override
    public Mono<Void> scale(String applicationName, Integer diskLimit, Integer memoryLimit, Integer instances) {
        return this.applicationsOperations.scale(applicationName, diskLimit, memoryLimit, instances)
                .doOnSubscribe(aVoid -> {
                    log.info("Scaling application", applicationName);
                    log.debug("New disk limit:", diskLimit);
                    log.debug("New memory limit:", memoryLimit);
                    log.debug("New number of instances:", instances);
                })
                .doOnSuccess(aVoid -> log.verbose("Scaling application", applicationName, "completed"));
    }

    @Override
    public Mono<Void> addEnvironmentVariable(String applicationName, String variableName, String variableValue) {
        return this.applicationsOperations.addEnvironmentVariable(applicationName, variableName, variableValue)
                .doOnSubscribe(aVoid -> {
                    log.info("Adding environment variable",
                            variableName,
                            "with value",
                            variableValue,
                            "to app",
                            applicationName);
                }).doOnSuccess(aVoid -> log.verbose("Adding environment variable",
                        variableName,
                        "with value",
                        variableValue ,
                        "to app",
                        applicationName,
                        "completed")
                );
    }

    @Override
    public Mono<Void> removeEnvironmentVariable(String applicationName, String variableName) {
        return this.applicationsOperations.removeEnvironmentVariable(applicationName, variableName)
                .doOnSubscribe(aVoid -> log.info("Removing environment variable",
                        variableName,
                        "from app",
                        applicationName))
                .doOnSuccess(aVoid -> log.verbose("Removing environment variable",
                        variableName,
                        "from app",
                        applicationName,
                        "completed"));
    }

    @Override
    public Mono<Void> setHealthCheck(String applicationName, ApplicationHealthCheck healthCheckType) {
        return applicationsOperations.setHealthCheck(applicationName, healthCheckType)
                .doOnSubscribe(aVoid -> log.info(
                        "Setting health check type for app", applicationName, "to", healthCheckType))
                .doOnSuccess(aVoid -> log.verbose("Setting health check type for app", applicationName, "completed"));
    }

    @Override
    public Mono<Void> bindToService(String applicationName, String serviceName) {
        return this.applicationsOperations.bindToService(applicationName, serviceName)
                .doOnSubscribe(aVoid -> log.info("Binding application", applicationName, "to service", serviceName))
                .doOnSuccess(aVoid -> log.verbose(
                        "Binding application", applicationName, "to service", serviceName, "completed"));
    }

    @Override
    public Mono<Void> unbindFromService(String applicationName, String serviceName) {
        return this.applicationsOperations.unbindFromService(applicationName, serviceName)
                .doOnSubscribe(aVoid -> log.info("Unbinding app", applicationName, "from service", serviceName))
                .doOnSuccess(aVoid -> log.verbose(
                        "Unbinding app", applicationName, "from service", serviceName, "completed"));
    }

    @Override
    public Mono<Void> addRoute(String applicationName, String route) {
        return this.applicationsOperations.addRoute(applicationName, route)
                .doOnSubscribe(aVoid -> log.info("Adding route", route, "to app", applicationName))
                .doOnSuccess(aVoid -> log.verbose(
                        "Adding route", route, "to app", applicationName, "completed"));
    }

    @Override
    public Mono<Void> removeRoute(String applicationName, String route) {
        return this.applicationsOperations.removeRoute(applicationName, route)
                .doOnSubscribe(aVoid -> log.info("Removing route", route, "from app", applicationName))
                .doOnSuccess(aVoid -> log.info(
                        "Removing route", route, "from app", applicationName, "completed"));
    }
}
