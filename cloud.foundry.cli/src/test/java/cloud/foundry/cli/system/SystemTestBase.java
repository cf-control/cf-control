package cloud.foundry.cli.system;

import cloud.foundry.cli.services.BaseController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

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
public class SystemTestBase {
    @BeforeAll
    private static void installCustomSecurityManager() {
        System.setSecurityManager(new PreventExitSecurityManager());
    }

    @AfterAll
    private static void restoreOriginalSecurityManager() {
        System.setSecurityManager(null);
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
        RunResult runResult = new RunResult(exitCode, streamContents);
        return runResult;
    }

    /**
     * Run base controller with provided arguments as well as the credentials/target information for the hosted system
     * test environment.
     * The data are fetched from the environment, and need to be set during test runs. Otherwise, an exception
     * is thrown.
     * @param argumentsBuilder arguments builder to which the data from the environment are appended
     * @throws IllegalArgumentException in case any of the environment variables are not set
     * @returns run result
     */
    protected RunResult runBaseControllerWithCredentialsFromEnvironment(ArgumentsBuilder argumentsBuilder) {
        // these environment variables are supposed to be defined in the test environment, and need to be set to
        // the correct values (e.g., in the run configuration and on Travis CI)
        argumentsBuilder.addOptionFromEnvironmentVariable("-u", "CF_USERNAME");
        argumentsBuilder.addOptionFromEnvironmentVariable("-p", "CF_PASSWORD");
        argumentsBuilder.addOptionFromEnvironmentVariable("-s", "CF_SPACE");
        argumentsBuilder.addOptionFromEnvironmentVariable("-o", "CF_ORGANIZATION");
        argumentsBuilder.addOptionFromEnvironmentVariable("-a", "CF_API_ENDPOINT");

        return runBaseControllerWithArgs(argumentsBuilder.build());
    }
}
