package cloud.foundry.cli.operations.applications;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ApplicationsOperationsLogging implements ApplicationsOperations {

    private static final Log log = Log.getLog(ApplicationsOperations.class);

    ApplicationsOperations applicationsOperations;

    public ApplicationsOperationsLogging(@Nonnull ApplicationsOperations applicationsOperations) {
        checkNotNull(applicationsOperations);

        this.applicationsOperations = applicationsOperations;
    }


    @Override
    public Mono<Map<String, ApplicationBean>> getAll() {
        return this.applicationsOperations.getAll()
                .doOnSubscribe(subscription -> log.debug("Querying all applications"))
                .doOnSuccess(stringApplicationBeanMap -> log.debug("All applications fetched"));
    }

    @Override
    public Mono<Void> remove(String applicationName) {
        return this.applicationsOperations.remove(applicationName)
                .doOnSubscribe(aVoid -> log.info("Removing app: ", applicationName))
                .doOnSuccess(aVoid -> log.info("App removed: ", applicationName));
    }

    @Override
    public Mono<Void> update(String appName, ApplicationBean bean, boolean shouldStart) {
        return this.applicationsOperations.update(appName, bean, shouldStart)
                .doOnSubscribe(aVoid -> log.info("Updating app: ", appName))
                .doOnSuccess(aVoid -> log.info("App updated: ", appName));
    }

    @Override
    public Mono<Void> create(String appName, ApplicationBean bean, boolean shouldStart) {
        return this.applicationsOperations.create(appName, bean, shouldStart)
                .doOnSubscribe(subscription -> log.debug("Creating app:", appName))
                .doOnSuccess(aVoid -> log.info("App created:", appName));
    }

    @Override
    public Mono<Void> rename(String newName, String currentName) {
        return this.applicationsOperations.rename(newName, currentName)
                .doOnSubscribe(aVoid -> {
                    log.debug("Rename application:", currentName);
                    log.debug("With new name:", newName); })
                .doOnSuccess(aVoid -> log.info("Application renamed from", currentName, "to", newName));
    }

    @Override
    public Mono<Void> scale(String applicationName, Integer diskLimit, Integer memoryLimit, Integer instances) {
        return this.applicationsOperations.scale(applicationName, diskLimit, memoryLimit, instances)
                .doOnSubscribe(aVoid -> {
                    log.debug("Scale app:", applicationName);
                    log.debug("With new disk limit:", diskLimit);
                    log.debug("With new memory limit:", memoryLimit);
                    log.debug("With new number of instances:", instances); })
                .doOnSuccess(aVoid -> log.info("Application", applicationName, "was scaled"));
    }

    @Override
    public Mono<Void> addEnvironmentVariable(String applicationName, String variableName, String variableValue) {
        return this.applicationsOperations.addEnvironmentVariable(applicationName, variableName, variableValue)
                .doOnSubscribe(aVoid -> {
                    log.debug("Adding environment variable",
                            variableName,
                            "with value",
                            variableValue,
                            "for app:",
                            applicationName); })
                .doOnSuccess(aVoid -> log.info("Environment variable",
                        variableName,
                        "with value",
                        variableValue ,
                        "was added to the app",
                        applicationName));
    }

    @Override
    public Mono<Void> removeEnvironmentVariable(String applicationName, String variableName) {
        return this.applicationsOperations.removeEnvironmentVariable(applicationName, variableName)
                .doOnSubscribe(aVoid -> log.debug("Removing environment variable",
                        variableName,
                        "for app:",
                        applicationName))
                .doOnSuccess(aVoid -> log.info("Environment variable",
                        variableName,
                        "was removed from the app",
                        applicationName));
    }

    @Override
    public Mono<Void> setHealthCheck(String applicationName, ApplicationHealthCheck healthCheckType) {
        return applicationsOperations.setHealthCheck(applicationName, healthCheckType)
                .doOnSubscribe(aVoid -> {
                    log.debug("Set health check type for app:", applicationName);
                    log.debug("With health check type:", healthCheckType); })
                .doOnSuccess(aVoid -> log.info("The health check type of the app", applicationName,
                        "was set to", healthCheckType));
    }

    @Override
    public Mono<Void> bindToService(String applicationName, String serviceName) {
        return this.applicationsOperations.bindToService(applicationName, serviceName)
                .doOnSuccess(aVoid -> {
                    log.debug("Bind app:", applicationName);
                    log.debug("To service:", serviceName); })
                .doOnSuccess(aVoid -> log.info("Bound the app", applicationName,
                        "to the service", serviceName));
    }

    @Override
    public Mono<Void> unbindFromService(String applicationName, String serviceName) {
        return this.applicationsOperations.unbindFromService(applicationName, serviceName)
                .doOnSuccess(aVoid -> {
                    log.debug("Unbind app:", applicationName);
                    log.debug("From service:", serviceName); })
                .doOnSuccess(aVoid -> log.info("Unbound the app", applicationName,
                        "from the service", serviceName));
    }

    @Override
    public Mono<Void> addRoute(String applicationName, String route) {
        return this.applicationsOperations.addRoute(applicationName, route)
                .doOnSuccess(aVoid -> {
                    log.debug("Add route:", route);
                    log.debug("To app:", applicationName); })
                .doOnSuccess(aVoid -> log.info("Added the route", route,
                        "to the application", applicationName));
    }

    @Override
    public Mono<Void> removeRoute(String applicationName, String route) {
        return this.applicationsOperations.removeRoute(applicationName, route)
                .doOnSuccess(aVoid -> {
                    log.debug("Remove route:", route);
                    log.debug("From app:", applicationName); })
                .doOnSuccess(aVoid -> log.info("Removed the route", route,
                        "from the application", applicationName));
    }
}
