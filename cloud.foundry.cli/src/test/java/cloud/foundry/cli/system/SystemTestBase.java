package cloud.foundry.cli.system;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.services.BaseController;
import cloud.foundry.cli.services.LoginCommandOptions;
import cloud.foundry.cli.system.util.*;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for all system tests. Provides many convenience methods in order to make test run simulations really
 * easy for deriving classes.
 *
 * Please beware that all tests deriving from this class will experience that calls to System.exit(...) do not lead to
 * a termination of the program but a {@link SystemExitException}.
 *
 * Please check the runBaseController* methods, which eliminate a lot of boiler plate code usually required for system
 * test run simulations.
 */
public abstract class SystemTestBase {

    /*
     * these environment variables are supposed to be defined in the test environment, and need to be set to
     * the correct values (e.g., in the run configuration and on Travis CI)
     */
    private static final String CF_USERNAME = "CF_USERNAME";
    private static final String CF_PASSWORD = "CF_PASSWORD";
    private static final String CF_SPACE = "CF_SPACE";
    private static final String CF_ORGANIZATION = "CF_ORGANIZATION";
    private static final String CF_API_ENDPOINT = "CF_API_ENDPOINT";

    // collection of all environment variables that are not defined in the system environment
    private static final List<String> undefinedEnvironmentVariables = new LinkedList<>();

    // the space configurator will remain null if there are any undefined environment variables
    private static final SpaceConfigurator spaceConfigurator;

    // should usually be null, but we'll store whatever is set before overwriting it to be on the safe side
    private static SecurityManager cachedOriginalSecurityManager = null;

    private static final String cfUsernameValue;
    private static final String cfPasswordValue;
    private static final String cfSpaceValue;
    private static final String cfOrganizationValue;
    private static final String cfApiEndpointValue;

    public static String getCfSpaceValue() {
        return cfSpaceValue;
    }

    public static String getCfOrganizationValue() {
        return cfOrganizationValue;
    }

    public static String getCfApiEndpointValue() {
        return cfApiEndpointValue;
    }

    public static SpaceConfigurator getSpaceConfigurator() {
        checkSpaceConfiguratorIsValid();
        return spaceConfigurator;
    }

    static {
        /*
         * Reads and stores the values of environment variables, that are supposed to be defined. In case they are all
         * defined, this constructor also initializes the space configurator.
         */

        cfUsernameValue = readValueOfEnvironmentVariable(CF_USERNAME);
        cfPasswordValue = readValueOfEnvironmentVariable(CF_PASSWORD);
        cfSpaceValue = readValueOfEnvironmentVariable(CF_SPACE);
        cfOrganizationValue = readValueOfEnvironmentVariable(CF_ORGANIZATION);
        cfApiEndpointValue = readValueOfEnvironmentVariable(CF_API_ENDPOINT);

        // only when all env vars are available, the space configurator is populated
        // it isn't needed all the time, and this pattern allows to run such tests without having to set all the
        // environment variables correctly
        if (undefinedEnvironmentVariables.isEmpty()) {
            // setup login command options for initialization of the cloud foundry operations instance
            LoginCommandOptions loginCommandOptions = new LoginCommandOptions();
            loginCommandOptions.setUserName(cfUsernameValue);
            loginCommandOptions.setPassword(cfPasswordValue);
            loginCommandOptions.setSpace(cfSpaceValue);
            loginCommandOptions.setOrganization(cfOrganizationValue);
            loginCommandOptions.setApiHost(cfApiEndpointValue);

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginCommandOptions);

            // create operations instances that are needed by the space configurator
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            spaceConfigurator = new SpaceConfigurator(servicesOperations, applicationsOperations);
        } else {
            spaceConfigurator = null;
        }
    }

    protected static void checkSpaceConfiguratorIsValid() {
        if (!undefinedEnvironmentVariables.isEmpty()) {
            throw new IllegalStateException("The environment variables " +
                    Arrays.toString(undefinedEnvironmentVariables.toArray()) + " are not defined");
        }

        if (spaceConfigurator == null) {
            throw new IllegalStateException("space configurator has not been created for unknown reason");
        }
    }

    /**
     * Default constructor. Doesn't have to do anything, really
     */
    public SystemTestBase() {}

    /**
     * Little helper that reads environment variables and puts unknown ones in a list (for better error messages).
     * @param environmentVariable variable to fetch
     * @return environment variable's value or null if it isn't set
     */
    private static String readValueOfEnvironmentVariable(String environmentVariable) {
        String environmentVariableValue = System.getenv(environmentVariable);
        if (environmentVariableValue == null) {
            undefinedEnvironmentVariables.add(environmentVariable);
        }
        return environmentVariableValue;
    }

    @BeforeAll
    private static void installCustomSecurityManager() {
        cachedOriginalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new PreventExitSecurityManager());
    }

    @AfterAll
    private static void restoreOriginalSecurityManager() {
        System.setSecurityManager(cachedOriginalSecurityManager);
        cachedOriginalSecurityManager = null;
    }

    /**
     * Run base controller with provided arguments. Simulates a normal program execution.
     * @param arguments arguments to run application with
     * @return run result
     */
    protected RunResult runBaseControllerWithArgs(String[] arguments) {
        // capture stdout/stderr contents
        StreamManager streamManager = new StreamManager();
        streamManager.installNewStreams();

        int exitCode = Integer.MIN_VALUE;

        try {
            BaseController.main(arguments);
        } catch (SystemExitException e) {
            exitCode = e.getExitCode();
        }

        assert exitCode != Integer.MIN_VALUE;

        // reset streams and fetch contents
        streamManager.restoreOldStreams();
        StreamContents streamContents = streamManager.getContents();

        // return result
        return new RunResult(exitCode, streamContents);
    }

    /**
     * Run base controller with provided arguments as well as the credentials/target information for the hosted system
     * test environment.
     * The data are fetched from the environment, and need to be set during test runs. Otherwise, an exception
     * is thrown.
     * @param argumentsBuilder arguments builder to which the data from the environment are appended
     * @throws IllegalStateException in case any of the environment variables are not set
     * @return run result
     */
    protected RunResult runBaseControllerWithCredentialsFromEnvironment(ArgumentsBuilder argumentsBuilder) {
        checkSpaceConfiguratorIsValid();

        argumentsBuilder.addOption("-u", cfUsernameValue);
        argumentsBuilder.addOption("-p", cfPasswordValue);
        argumentsBuilder.addOption("-s", cfSpaceValue);
        argumentsBuilder.addOption("-o", cfOrganizationValue);
        argumentsBuilder.addOption("-a", cfApiEndpointValue);

        return runBaseControllerWithArgs(argumentsBuilder.build());
    }
}
