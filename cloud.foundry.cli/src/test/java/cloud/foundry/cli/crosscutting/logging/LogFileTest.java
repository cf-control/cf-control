package cloud.foundry.cli.crosscutting.logging;

import cloud.foundry.cli.services.BaseController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for --log-file feature.
 */
public class LogFileTest {
    /**
     * Thrown by custom security manager whenever System.exit(...) is called.
     */
    protected static class SystemExitException extends SecurityException {
        private final int exitCode;

        public SystemExitException(int exitCode) {
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }

    /**
     * Custom security manager which prevents the JVM from exiting.
     * Instead, it raises a {@link SystemExitException} when there's a call to System.exit(...).
     */
    private static class PreventExitSecurityManager extends SecurityManager {
        // these checkPermission overrides are needed for some reason...
        @Override
        public void checkPermission(Permission perm) {}

        @Override
        public void checkPermission(Permission perm, Object context) {}

        @Override
        public void checkExit(int status) {
            super.checkExit(status);
            throw new SystemExitException(status);
        }
    }

    @BeforeAll
    private static void installCustomSecurityManager() {
        System.setSecurityManager(new PreventExitSecurityManager());
    }

    @AfterAll
    private static void restoreOriginalSecurityManager() {
        System.setSecurityManager(null);
    }

    // we use this directory to store the log files in
    // since it's an instance member, it should be cleaned up and recreated between the tests
    @TempDir
    File tempDir;

    private void runBaseControllerWithArgs(List<String> args) {
        // to simulate a main() run, we need a regular String array
        String[] argsArray = new String[args.size()];
        args.toArray(argsArray);

        BaseController.main(argsArray);
    }

    @Test
    public void testBaseController() throws ParserConfigurationException, IOException, SAXException {
        final List<String> args = new ArrayList<>();

        // we expect the application to write to this log file
        final File logFile = Paths.get(tempDir.toString(), "test.log").toFile();

        args.add("--log-file");
        args.add(logFile.getAbsolutePath());

        // let's be as verbose as possible
        args.add("-d");

        // first random command; doesn't really matter, it won't work anyway
        args.add("get");
        args.add("space-developers");

        // need to provide *all* required CLI options, otherwise the tool will error out even _before_ the log file
        // parameter could be evaluated
        // FIXME: can be removed once #92 has been merged
        args.add("-a");
        args.add("test");

        args.add("-o");
        args.add("test");

        args.add("-s");
        args.add("test");

        int exitCode = -1;

        try {
            runBaseControllerWithArgs(args);
        } catch (SystemExitException e) {
            exitCode = e.getExitCode();
        }

        assert exitCode == 1;

        // let's see if the application wrote something into the log file
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Java XML log files contain references to some "logger.dtd" file, which we have to ignore
        documentBuilder.setEntityResolver((s, s1) -> {
            if (s1.endsWith("logger.dtd")) {
                return new InputSource(new StringReader(""));
            }
            return null;
        });

        // possibly due to System.exit calls, the log file ends without its closing </log> tag
        // since the issue is known, we can ignore it for now
        documentBuilder.parse(logFile);
    }

}
