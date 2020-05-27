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

    @Test
    public void testError() {
        String uniqueTestString = makeRandomTestString();

        Log.error(uniqueTestString);

        // make sure everything in err has been written to cache
        System.err.flush();

        // cache in separate variable to allow for introspection during debugging
        String stderr = stderrCache.toString();

        assert stderr.contains("SEVERE: " + uniqueTestString + "\n");
    }

    @Test
    public void testWarn() {
        String uniqueTestString = makeRandomTestString();

        Log.warn(uniqueTestString);

        // make sure everything in err has been written to cache
        System.out.flush();
        System.err.flush();

        // cache in separate variable to allow for introspection during debugging
        String stderr = stderrCache.toString();

        assert stderr.contains("WARNING: " + uniqueTestString + "\n");
    }

    @Test
    public void testInfo() {
        String uniqueTestString = makeRandomTestString();

        Log.info(uniqueTestString);

        // make sure everything in err has been written to cache
        System.out.flush();
        System.err.flush();

        // cache in separate variable to allow for introspection during debugging
        String stderr = stderrCache.toString();

        assert stderr.contains("INFO: " + uniqueTestString + "\n");
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

        assert stderr.contains("SEVERE: " + uniqueTestString + "\n");
        assert stderr.contains("java.lang.ArithmeticException: / by zero\n");
    }

}
