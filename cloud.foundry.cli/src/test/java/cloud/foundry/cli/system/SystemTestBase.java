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
     * @param args arguments to run application with
     * @throws SystemExitException in case System.exit is called
     */
    protected void runBaseControllerWithArgs(List<String> args) {
        // to simulate a main() run, we need a regular String array
        String[] argsArray = new String[args.size()];
        args.toArray(argsArray);

        BaseController.main(argsArray);
    }
}
