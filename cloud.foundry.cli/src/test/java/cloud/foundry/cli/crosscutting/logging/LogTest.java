package cloud.foundry.cli.crosscutting.logging;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

/**
 * Test for {@link Log}
 */
public class LogTest {

    private static ByteArrayOutputStream stdoutCache;
    private static ByteArrayOutputStream stderrCache;

    @BeforeAll
    private static void setUp() {
        stdoutCache = new ByteArrayOutputStream();
        stderrCache = new ByteArrayOutputStream();

        System.setOut(new PrintStream(stdoutCache));
        System.setErr(new PrintStream(stderrCache));
    }

    // no idea why, but running the tearDown after each test seems to break all but the first test
    // therefore, we run it once for now
    @AfterAll
    private static void tearDown() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
    }

    private static String makeRandomTestString() {
        // from '0' to 'z'
        int leftLimit = 0x30;
        int rightLimit = 0x7a;
        int length = 32;

        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static String getStderr() {
        // make sure everything in err has been written to cache
        System.err.flush();

        // cache in separate variable to allow for introspection during debugging
        String stderr = stderrCache.toString();

        return stderr;
    }

    @Test
    public void testError() {
        String uniqueTestString = makeRandomTestString();

        // error messages should be available on the default loglevel
        Log.setDefaultLogLevel();

        Log.error(uniqueTestString);

        assert getStderr().contains("SEVERE: " + uniqueTestString);
    }

    @Test
    public void testWarn() {
        String uniqueTestString = makeRandomTestString();

        // warning messages should be available on the default loglevel
        Log.setDefaultLogLevel();

        Log.warn(uniqueTestString);

        assert getStderr().contains("WARNING: " + uniqueTestString);
    }

    @Test
    public void testInfo() {
        String uniqueTestString = makeRandomTestString();

        // info messages should be available on the default loglevel
        Log.setDefaultLogLevel();

        Log.info(uniqueTestString);

        assert getStderr().contains("INFO: " + uniqueTestString);
    }

    @Test
    public void testVerbose() {
        String uniqueTestString = makeRandomTestString();

        // verbose logs should *not* be available on the default loglevel
        Log.setDefaultLogLevel();

        Log.verbose(uniqueTestString);

        assert !getStderr().contains("FINE: " + uniqueTestString);

        // but they should be visible in verbose mode
        Log.setVerboseLogLevel();

        Log.verbose(uniqueTestString);

        assert getStderr().contains("FINE: " + uniqueTestString);
    }

    @Test
    public void testDebug() {
        String uniqueTestString = makeRandomTestString();

        // debug logs should *not* be available on the default loglevel
        Log.setDefaultLogLevel();

        Log.verbose(uniqueTestString);

        assert !getStderr().contains("FINER: " + uniqueTestString);

        // but they should be visible in debug mode
        Log.setDebugLogLevel();

        Log.debug(uniqueTestString);

        assert getStderr().contains("FINER: " + uniqueTestString);
    }

    @Test
    public void testException() {
        String uniqueTestString = makeRandomTestString();

        // generate exception
        try {
            int i = 1 / 0;
        } catch (ArithmeticException e) {
            Log.exception(e, uniqueTestString);
        }

        // make sure everything in err has been written to cache
        System.out.flush();
        System.err.flush();

        // cache in separate variable to allow for introspection during debugging
        String stderr = stderrCache.toString();

        assert stderr.contains("SEVERE: " + uniqueTestString);
        assert stderr.contains("java.lang.ArithmeticException: / by zero");
    }

}
