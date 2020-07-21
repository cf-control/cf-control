package cloud.foundry.cli.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import cloud.foundry.cli.crosscutting.mapping.beans.*;
import cloud.foundry.cli.system.util.ArgumentsBuilder;
import cloud.foundry.cli.system.util.RunResult;
import cloud.foundry.cli.system.util.SpaceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Test for {@link cloud.foundry.cli.services.DiffController}, specifically the default diff command which diffs
 * the given config with the state of the live system.
 */
public class DiffCommandTest extends SystemTestBase {

    @Test
    public void testMissingServiceAndApplication() {
        //given
        try (SpaceManager spaceManager = createSpaceWithRandomName()) {
            String expected = "  spec:\n" +
                    "    services:\n" +
                    "+     testService:\n" +
                    "+       plan: standard\n" +
                    "+       service: app-autoscaler\n" +
                    "    apps:\n" +
                    "+     testApp:\n" +
                    "+       manifest:\n" +
                    "+         buildpack: https://github.com/cloudfoundry/python-buildpack.git\n" +
                    "+         disk: 1024\n" +
                    "+         healthCheckType: HTTP\n" +
                    "+         instances: 2\n" +
                    "+         memory: 512\n" +
                    "+         noRoute: true\n" +
                    "+         stack: cflinuxfs3\n" +
                    "+       path: src/test/resources/system/demo-python-app\n" +
                    "  target:\n" +
                    "+   space: someSpace\n" +
                    "-   space: " + spaceManager.getSpaceName() + System.lineSeparator();

            //when
            ArgumentsBuilder args = new ArgumentsBuilder()
                    .addArgument("diff")
                    .addOption("-y", "src/test/resources/system/diff/testConfig.yml");
            RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);
            String outContent = runResult.getStreamContents().getStdoutContent();

            //then
            assertThat(outContent, is(expected));
            assertThat(runResult.getStreamContents().getStderrContent().length(), is(greaterThan(0)));
        }
    }

    @Test
    public void testWithChangesInServiceAndApplication() {
        //given
        try (SpaceManager spaceManager = createSpaceWithRandomName()) {
            String expected = "  spec:\n" +
                    "    services:\n" +
                    "      testService:\n" +
                    "+       service: app-autoscaler\n" +
                    "-       service: elephantsql\n" +
                    "+       plan: standard\n" +
                    "-       plan: turtle\n" +
                    "    apps:\n" +
                    "      testApp:\n" +
                    "        manifest:\n" +
                    "+         healthCheckType: http\n" +
                    "-         healthCheckType: port\n" +
                    "+         instances: 2\n" +
                    "-         instances: 1\n" +
                    "+         memory: 512\n" +
                    "-         memory: 1024\n" +
                    "  target:\n" +
                    "+   space: someSpace\n" +
                    "-   space: " + spaceManager.getSpaceName() + System.lineSeparator();

            requestApplicationCreation(spaceManager);
            requestServiceCreation(spaceManager);
            spaceManager.createRequestedEntities();

            //when
            ArgumentsBuilder args = new ArgumentsBuilder()
                    .addArgument("diff")
                    .addOption("-y", "src/test/resources/system/diff/testConfig.yml");
            RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);
            String outContent = runResult.getStreamContents().getStdoutContent();

            //then
            assertThat(outContent, is(expected));
            assertThat(runResult.getStreamContents().getStderrContent().length(), is(greaterThan(0)));
        }
    }

    @Test
    public void testDiffWithGetResult(@TempDir Path tempDir) throws IOException {
        //given
        try (SpaceManager spaceManager = createSpaceWithRandomName()) {
            requestApplicationCreation(spaceManager);
            requestServiceCreation(spaceManager);
            spaceManager.createRequestedEntities();

            //call get command and write its output in a temporary file
            //the file will be used as an input for the diff command
            ArgumentsBuilder args = new ArgumentsBuilder()
                    .addArgument("get");
            RunResult runResultGet = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);
            String outContentGet = runResultGet.getStreamContents().getStdoutContent();

            Path pathGetResult = tempDir.resolve("getResult.yml");
            Files.write(pathGetResult, outContentGet.getBytes());

            //when
            args = new ArgumentsBuilder()
                    .addArgument("diff")
                    .addOption("-y", pathGetResult.toAbsolutePath().toString());
            RunResult runResultDiff = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);
            String outContentDiff = runResultDiff.getStreamContents().getStdoutContent();

            //then
            assertThat(outContentDiff, is("There are no differences." + System.lineSeparator()));
            assertThat(runResultDiff.getStreamContents().getStderrContent().length(), is(greaterThan(0)));
        }
    }

    private void requestApplicationCreation(SpaceManager spaceManager) {
        ApplicationBean application = new ApplicationBean();
        application.setPath("src/test/resources/system/demo-python-app");
        ApplicationManifestBean manifest = new ApplicationManifestBean();
        application.setManifest(manifest);
        manifest.setBuildpack("https://github.com/cloudfoundry/python-buildpack.git");
        spaceManager.requestCreationOfApplication("testApp", application);
    }

    private void requestServiceCreation(SpaceManager spaceManager) {
        ServiceBean service = new ServiceBean();
        service.setService("elephantsql");
        service.setPlan("turtle");
        spaceManager.requestCreationOfService("testService", service);
    }

}
