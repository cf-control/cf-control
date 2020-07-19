package cloud.foundry.cli.operations.services;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

public class ServicesOperationsLogging implements ServicesOperations {

    private final Log log;

    private ServicesOperations servicesOperations;

    public ServicesOperationsLogging(@Nonnull ServicesOperations servicesOperations) {
        this.servicesOperations = servicesOperations;
        this.log = Log.getLog(ServicesOperations.class);
    }

    @Override
    public Mono<Map<String, ServiceBean>> getAll() {
        return this.servicesOperations.getAll()
                .doOnSubscribe(subscription -> log.debug("Querying all services"))
                .doOnSuccess(stringApplicationBeanMap -> log.debug("Querying all services finished"));
    }

    @Override
    public Mono<Void> create(String serviceInstanceName, ServiceBean serviceBean) {
        return this.servicesOperations.create(serviceInstanceName, serviceBean)
                .doOnSubscribe(aVoid -> {
                    log.info("Creating service", serviceInstanceName);
                    log.debug("Service bean:", serviceBean);
                })
                .doOnSuccess(aVoid -> log.verbose("Creating service", serviceInstanceName, "completed"));
    }

    @Override
    public Mono<Void> rename(String newName, String currentName) {
        return this.servicesOperations.rename(newName, currentName)
                .doOnSubscribe(aVoid -> log.info("Renaming service", currentName, "to", newName))
                .doOnSuccess(aVoid -> log.verbose("Renaming service", currentName, "to", newName, "completed"));
    }

    @Override
    public Mono<Void> update(String serviceInstanceName, ServiceBean serviceBean) {
        return this.servicesOperations.update(serviceInstanceName, serviceBean)
                .doOnSubscribe(subscription -> {
                    log.info("Updating tags and/or plan for service", serviceInstanceName);
                    log.debug("Service bean:", serviceBean);
                })
                .doOnSuccess(aVoid -> log.verbose(
                        "Updating tags and/or plan for service", serviceInstanceName, "completed"));
    }

    @Override
    public Mono<Void> remove(String serviceInstanceName) {
        return this.servicesOperations.remove(serviceInstanceName)
                .doOnSubscribe(aVoid -> log.info("Removing service", serviceInstanceName))
                .doOnSuccess(aVoid -> log.verbose("Removing service", serviceInstanceName, "completed"));
    }

    @Override
    public Flux<Void> deleteKeys(String serviceInstanceName) {
        return this.servicesOperations.deleteKeys(serviceInstanceName)
                .doOnSubscribe(subscription -> log.info(
                        "Deleting keys from service " + serviceInstanceName))
                .doOnComplete(() -> log.verbose(
                        "Deleting keys from service " + serviceInstanceName, "completed"));
    }

    @Override
    public Flux<Void> unbindApps(String serviceInstanceName) {
        return this.servicesOperations.unbindApps(serviceInstanceName)
                .doOnSubscribe(aVoid -> log.info(
                        "Unbinding all applications from service instance", serviceInstanceName))
                .doOnComplete(() -> log.verbose(
                        "Unbinding all applications from service instance", serviceInstanceName, "completed"));
    }

    @Override
    public Flux<Void> unbindRoutes(String serviceInstanceName) {
        return this.servicesOperations.unbindRoutes(serviceInstanceName)
                .doOnSubscribe(aVoid -> log.info("Unbinding all routes from service instance", serviceInstanceName))
                .doOnComplete(() -> log.verbose(
                        "Unbinding all routes from service instance", serviceInstanceName, "completed"));
    }
}
