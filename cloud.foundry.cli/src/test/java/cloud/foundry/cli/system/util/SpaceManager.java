package cloud.foundry.cli.system.util;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceOperations;
import cloud.foundry.cli.operations.applications.DefaultApplicationsOperations;
import cloud.foundry.cli.operations.services.DefaultServicesOperations;
import cloud.foundry.cli.services.LoginCommandOptions;
import org.apache.commons.lang3.RandomStringUtils;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.routes.DeleteOrphanedRoutesRequest;
import org.cloudfoundry.operations.spaces.DeleteSpaceRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * This class is responsible to configure a space for the purpose of system testing. It is able to manipulate the
 * spaces, services and applications of a cf instance as needed by a particular system test.
 * The class supports the try-with-resources idiom, and users are encouraged to use it like that.
 * https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
 */
public class SpaceManager implements AutoCloseable {
    /*
     * these environment variables are supposed to be defined in the test environment, and need to be set to
     * the correct values (e.g., in the run configuration and on Travis CI)
     */
    private static final String CF_USERNAME_ENV_VAR_NAME = "CF_USERNAME";
    private static final String CF_PASSWORD_ENV_VAR_NAME = "CF_PASSWORD";
    private static final String CF_ORGANIZATION_ENV_VAR_NAME = "CF_ORGANIZATION";
    private static final String CF_API_ENDPOINT_ENV_VAR_NAME = "CF_API_ENDPOINT";

    private final HashMap<String, ServiceBean> servicesToCreate = new HashMap<>();
    private final HashMap<String, ApplicationBean> applicationsToCreate = new HashMap<>();

    // name of space to manage, defined by the user of this class
    // ideally, it's a random, unpredictable name to ensure that tests can be executed in parallel
    private final String spaceName;

    // populated once space is created
    private DefaultCloudFoundryOperations cfOperations = null;
    private ServicesOperations servicesOperations = null;
    private ApplicationsOperations applicationsOperations = null;

    public static String makeRandomString() {
        // 30 characters should be unique enough
        return RandomStringUtils.randomAlphanumeric(30);
    }

    /**
     * Create manager for given space name. The space should not exist yet, and should not be used for
     * productive purposes. It's best practice to use a random name to allow parallel executions of system tests.
     * @param spaceName name of space to be managed by this instance
     */
    public SpaceManager(String spaceName) {
        this.spaceName = spaceName;
        createSpace();
    }

    /**
     * To be called every time the system environment is accessed.
     */
    private void assertAllRequiredEnvVarsAvailable() {
        ArrayList<String> undefinedEnvironmentVariables = new ArrayList<>();

        // query environment variables, remembering all the ones which are not defined
        // fetching data from the environment is very "cheap", it'd be more annoying to keep them around
        // it adds a little complexity, but allows us to print them out to help the user set them
        for (String envVarName : new String[] {
                CF_USERNAME_ENV_VAR_NAME,
                CF_PASSWORD_ENV_VAR_NAME,
                CF_ORGANIZATION_ENV_VAR_NAME,
                CF_API_ENDPOINT_ENV_VAR_NAME,
        }) {
            String envVarValue = System.getenv(envVarName);
            if (envVarValue == null) {
                undefinedEnvironmentVariables.add(envVarName);
            }
        }

        if (!undefinedEnvironmentVariables.isEmpty()) {
            throw new IllegalStateException("The environment variables " +
                    Arrays.toString(undefinedEnvironmentVariables.toArray()) + " are not defined");
        }
    }

    private String safeGetenv(String envVarName) {
        // precondition
        assertAllRequiredEnvVarsAvailable();

        String value = System.getenv(envVarName);

        if (value == null) {
            throw new IllegalArgumentException("Environment variable " + envVarName + " is not set");
        }

        return value;
    }

    /**
     * @return CF username (as defined in environment variable)
     * @throws IllegalArgumentException if environment variable is not defined
     */
    public String getCfUsername() {
        return safeGetenv(CF_USERNAME_ENV_VAR_NAME);
    }

    /**
     * @return CF password (as defined in environment variable)
     * @throws IllegalArgumentException if environment variable is not defined
     */
    public String getCfPassword() {
        return safeGetenv(CF_PASSWORD_ENV_VAR_NAME);
    }

    /**
     * @return CF organization (as defined in environment variable)
     * @throws IllegalArgumentException if environment variable is not defined
     */
    public String getCfOrganization() {
        return safeGetenv(CF_ORGANIZATION_ENV_VAR_NAME);
    }

    /**
     * @return CF API endpoint (as defined in environment variable)
     * @throws IllegalArgumentException if environment variable is not defined
     */
    public String getCfApiEndpoint() {
        return safeGetenv(CF_API_ENDPOINT_ENV_VAR_NAME);
    }

    /**
     * @return CF space name
     */
    public String getSpaceName() {
        return spaceName;
    }

    private void createSpace() {
        if (servicesOperations != null || applicationsOperations != null) {
            throw new IllegalStateException("space has been created already");
        }

        // setup login command options for initialization of the cloud foundry operations instance
        LoginCommandOptions loginMixin = new LoginCommandOptions();
        loginMixin.setUserName(getCfUsername());
        loginMixin.setPassword(getCfPassword());
        loginMixin.setOrganization(getCfOrganization());
        loginMixin.setApiHost(getCfApiEndpoint());
        loginMixin.setSpace(spaceName);

        cfOperations = CfOperationsCreator.createCfOperations(loginMixin);

        // apparently, there are some strange race conditions with the login which we might run into when querying the
        // spaces directly
        // the CF client library is not the best quality and one can sink a lot of time into debugging these kinds of
        // problems
        // like in other modules, the problem can be "solved" by running different operations beforehand, or delaying
        // the execution by >= 1 second with some sleep()
        // TODO: find more elegant way to resolve issue
        SpaceOperations spaceOperations = new SpaceOperations(cfOperations);
        spaceOperations.getAll().block();

        // time to create the space
        spaceOperations.create(spaceName).block();

        // create operations instances that are needed by the space configurator
        servicesOperations = new DefaultServicesOperations(cfOperations);
        applicationsOperations = new DefaultApplicationsOperations(cfOperations);
    }

    /**
     * Registers a service that is desired be created on the space.
     * @param desiredServiceBean the bean of the desired service
     * @return random name used for the service
     */
    public String requestCreationOfService(ServiceBean desiredServiceBean) {
        String randomName = "service-" + makeRandomString();
        servicesToCreate.put(randomName, desiredServiceBean);
        return randomName;
    }

    /**
     * Registers an application that is desired be created on the space.
     * @param desiredApplicationBean the bean of the desired application
     * @return random name used for the application
     */
    public String requestCreationOfApplication(ApplicationBean desiredApplicationBean) {
        String randomName = "app-" + makeRandomString();
        applicationsToCreate.put(randomName, desiredApplicationBean);
        return randomName;
    }

    /**
     * Creates all previously registered desired services and applications on the space. After the creation process has
     * finished, the applications and services are not registered as desired anymore.
     * @throws RuntimeException or other subclasses of RuntimeException if any errors occur during the creation process
     */
    public void createRequestedEntities() {
        // FIXME if possible: Flux.merge would be faster but it leads to internal server errors on the cf instance
        Flux.concat(collectServiceCreationRequests()).blockLast();

        Flux.merge(collectApplicationCreationRequests()).blockLast();
    }

    private List<Mono<Void>> collectServiceCreationRequests() {
        if (servicesOperations == null) {
            throw new IllegalStateException("space has not been created yet");
        }

        List<Mono<Void>> resultingCreationRequests = servicesToCreate.entrySet().stream()
                .map(serviceEntry -> servicesOperations.create(serviceEntry.getKey(), serviceEntry.getValue()))
                .collect(Collectors.toList());

        return resultingCreationRequests;
    }

    private List<Mono<Void>> collectApplicationCreationRequests() {
        if (applicationsOperations == null) {
            throw new IllegalStateException("space has not been created yet");
        }

        List<Mono<Void>> resultingCreationRequests = applicationsToCreate.entrySet().stream()
                .map(applicationEntry ->
                        applicationsOperations.create(applicationEntry.getKey(), applicationEntry.getValue(), false))
                .collect(Collectors.toList());
        return resultingCreationRequests;
    }

    /**
     * Removes all previously registered desired services and applications on the space. After the removal process has
     * finished, the applications and services are not registered as desired anymore.
     * @throws RuntimeException or other subclasses of RuntimeException if any errors occur during the removal process
     */
    public void removeSpace() {
        cleanUpSpace();

        cfOperations.spaces().delete(
                DeleteSpaceRequest.builder()
                        .name(spaceName)
                        .build())
                .block();
    }

    private List<Mono<Void>> collectServiceRemovalRequests() {
        List<Mono<Void>> resultingRemovalRequests = servicesToCreate.keySet().stream()
                .map(serviceName -> servicesOperations.remove(serviceName))
                .collect(Collectors.toList());

        return resultingRemovalRequests;
    }

    private List<Mono<Void>> collectApplicationRemovalRequests() {
        List<Mono<Void>> resultingRemovalRequests = applicationsToCreate.keySet().stream()
                .map(applicationName -> applicationsOperations.remove(applicationName))
                .collect(Collectors.toList());

        return resultingRemovalRequests;
    }

    /**
     * Removes all services and applications on the space. This is completely independent from any registered desired
     * services or applications.
     * @throws RuntimeException or other subclasses of RuntimeException if any errors occur during the removal process
     */
    public void cleanUpSpace() {
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

        // make sure all routes are gone before
        cfOperations.routes().deleteOrphanedRoutes(DeleteOrphanedRoutesRequest.builder().build()).block();
    }

    /**
     * Convenience method. Appends all options required for a system test run simulation to log into Cloud Foundry.
     * @param argumentsBuilder arguments builder to be populated with options
     */
    public void appendLoginArgumentsToArgumentsBuilder(ArgumentsBuilder argumentsBuilder) {
        // fetching data from the environment has some veeery low runtime cost, so we just re-fetch the data here
        // storing them would be more work
        argumentsBuilder.addOption("-u", System.getenv(CF_USERNAME_ENV_VAR_NAME));
        argumentsBuilder.addOption("-p", System.getenv(CF_PASSWORD_ENV_VAR_NAME));
        argumentsBuilder.addOption("-a", System.getenv(CF_API_ENDPOINT_ENV_VAR_NAME));
        argumentsBuilder.addOption("-o", System.getenv(CF_ORGANIZATION_ENV_VAR_NAME));

        // the space name is only known to the instance anyway
        argumentsBuilder.addOption("-s", spaceName);
    }

    /**
     * Alias for removeSpace to fulfill {@link AutoCloseable} specification.
     */
    @Override
    public void close() {
        removeSpace();
    }
}
