package cloud.foundry.cli.crosscutting.logging;

import cloud.foundry.cli.system.util.ArgumentsBuilder;
import cloud.foundry.cli.system.util.RunResult;
import cloud.foundry.cli.system.SystemTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for --log-file feature.
 */
public class LogFileTest extends SystemTestBase {
    // we use this directory to store the log files in
    // since it's an instance member, it should be cleaned up and recreated between the tests
    @TempDir
    File tempDir;

    @Test
    public void testBaseController() throws ParserConfigurationException, IOException, SAXException {
        final List<String> args = new ArrayList<>();

        // we expect the application to write to this log file
        final File logFile = Paths.get(tempDir.toString(), "test.log").toFile();

        String[] arguments = new ArgumentsBuilder()
                .addOption("--log-file", logFile.getAbsolutePath())

                // let's be as verbose as possible
                .addArgument("-d")

                // first random command; doesn't really matter, it won't work anyway
                .addArgument("get")
                .addArgument("space-developers")

                // need to provide *all* required CLI options, otherwise the tool will error out even _before_ the log
                // file parameter could be evaluated
                .addOption("-a", "test")
                .addOption("-o", "test")
                .addOption("-s", "test")

                .build();

        RunResult runResult = runBaseControllerWithArgs(arguments);

        assert runResult.getExitCode() == 1;

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
