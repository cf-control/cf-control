package cloud.foundry.cli.system;

import cloud.foundry.cli.services.BaseController;
import cloud.foundry.cli.system.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

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

    // should usually be null, but we'll store whatever is set before overwriting it to be on the safe side
    private static SecurityManager cachedOriginalSecurityManager = null;

    /**
     * Creates a space manager for a space with a fully random name, and creates the space.
     * The space manager can be used to interact with the space, create entities remotely and to later clean it up
     * and remove it again.
     * Note: it's recommended *not* to use setUp/tearDown methods to manage this instance; a try-finally setup
     * more likely ensures that the space can be cleaned up in case there's unexpected issues
     * @return space manager for space with random name
     */
    protected static SpaceManager createSpaceWithRandomName() {
        String spaceName = "test-space-" + SpaceManager.makeRandomString();
        SpaceManager spaceManager = new SpaceManager(spaceName);
        return spaceManager;
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
     * @param argumentsBuilder arguments builder to which the data from the environment are appended
     * @param spaceManager space manager to fetch login data from
     * @throws IllegalStateException in case any of the environment variables are not set
     * @return run result
     */
    protected RunResult runBaseControllerWithCredentialsFromEnvironment(
            ArgumentsBuilder argumentsBuilder,
            SpaceManager spaceManager
    ) {
        spaceManager.appendLoginArgumentsToArgumentsBuilder(argumentsBuilder);
        return runBaseControllerWithArgs(argumentsBuilder.build());
    }
}
