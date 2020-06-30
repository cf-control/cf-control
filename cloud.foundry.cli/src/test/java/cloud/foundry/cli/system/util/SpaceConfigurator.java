package cloud.foundry.cli.system.util;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * This class is responsible to configure a space for the purpose of system testing. It is able to manipulate the
 * spaces and applications of a cf instance as needed by a particular system test.
 */
public class SpaceConfigurator {

    private HashMap<String, ServiceBean> desiredServices = new HashMap<>();
    private HashMap<String, ApplicationBean> desiredApplications = new HashMap<>();

    private ServicesOperations servicesOperations;
    private ApplicationsOperations applicationsOperations;

    /**
     * Default constructor.
     * @param servicesOperations the service operations instance that is used to manipulate services on a space
     * @param applicationsOperations the applications operations instance that is used to manipulate apps on a space
     */
    public SpaceConfigurator(ServicesOperations servicesOperations, ApplicationsOperations applicationsOperations) {
        this.servicesOperations = servicesOperations;
        this.applicationsOperations = applicationsOperations;
    }

    /**
     * Registers a service that is desired be created on the space.
     * @param desiredServiceName the name of the desired service
     * @param desiredServiceBean the bean of the desired service
     */
    public void addDesiredService(String desiredServiceName, ServiceBean desiredServiceBean) {
        desiredServices.put(desiredServiceName, desiredServiceBean);
    }

    /**
     * Registers an application that is desired be created on the space.
     * @param desiredApplicationName the name of the desired application
     * @param desiredApplicationBean the bean of the desired application
     */
    public void addDesiredApplication(String desiredApplicationName, ApplicationBean desiredApplicationBean) {
        desiredApplications.put(desiredApplicationName, desiredApplicationBean);
    }

    /**
     * Creates all previously registered desired services and applications on the space. After the creation process has
     * finished, the applications and services are not registered as desired anymore.
     * @throws RuntimeException or other subclasses of RuntimeException if any errors occur during the creation process
     */
    public void configure() {
        // FIXME if possible: Flux.merge would be faster but it leads to internal server errors on the cf instance
        Flux.concat(collectServiceCreationRequests()).blockLast();

        Flux.merge(collectApplicationCreationRequests()).blockLast();
    }

    private List<Mono<Void>> collectServiceCreationRequests() {
        List<Mono<Void>> resultingCreationRequests = desiredServices.entrySet().stream()
                .map(serviceEntry -> servicesOperations.create(serviceEntry.getKey(), serviceEntry.getValue()))
                .collect(Collectors.toList());

        desiredServices.clear();

        return resultingCreationRequests;
    }

    private List<Mono<Void>> collectApplicationCreationRequests() {
        List<Mono<Void>> resultingCreationRequests = desiredApplications.entrySet().stream()
                .map(applicationEntry ->
                        applicationsOperations.create(applicationEntry.getKey(), applicationEntry.getValue(), false))
                .collect(Collectors.toList());

        desiredApplications.clear();

        return resultingCreationRequests;
    }

    /**
     * Removes ALL services and applications on the space. This is completely independent from any registered desired
     * services or applications.
     * @throws RuntimeException or other subclasses of RuntimeException if any errors occur during the removal process
     */
    public void clear() {
        // these references will point to sets containing the name of all applications/services of the cf instance
        AtomicReference<Set<String>> applicationsToRemove = new AtomicReference<>(null);
        AtomicReference<Set<String>> servicesToRemove = new AtomicReference<>(null);

        Mono<Set<String>> getApplicationNamesRequest = applicationsOperations.getAll()
                .map(Map::keySet)
                .doOnSuccess(applicationsToRemove::set);

        Mono<Set<String>> getServiceNamesRequest = servicesOperations.getAll()
                .map(Map::keySet)
                .doOnSuccess(servicesToRemove::set);

        // request the names of all applications and services on the cf instance
        Flux.merge(getApplicationNamesRequest, getServiceNamesRequest).blockLast();

        // it's assumed that the request was successful and that the references now point to the resulting name sets
        assert (applicationsToRemove.get() != null);
        assert (servicesToRemove.get() != null);

        // remove all applications of the cf instance by the previously collected application names
        Flux.merge(applicationsToRemove.get().stream()
                .map(applicationName -> applicationsOperations.remove(applicationName))
                .collect(Collectors.toList())).blockLast();

        // remove all services of the cf instance by the previously collected service names
        Flux.merge(servicesToRemove.get().stream()
                .map(serviceName -> servicesOperations.remove(serviceName))
                .collect(Collectors.toList())).blockLast();
    }
}
