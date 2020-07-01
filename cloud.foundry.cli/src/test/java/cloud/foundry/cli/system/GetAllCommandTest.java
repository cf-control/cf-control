package cloud.foundry.cli.system;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.*;
import cloud.foundry.cli.system.util.ArgumentsBuilder;
import cloud.foundry.cli.system.util.RunResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link cloud.foundry.cli.services.GetController}, specifically the default get command which fetches all
 * the information and outputs them in the same format we expect for the input.
 */
public class GetAllCommandTest extends SystemTestBase {

    @BeforeEach
    private void cleanUpSpaceBefore() {
        spaceConfigurator.clear();
    }

    @AfterEach
    private void cleanUpSpaceAfterwards() {
        spaceConfigurator.clear();
    }

    private void assertRootBeanIsValid(ConfigBean rootBean) {
        assert rootBean.getApiVersion().startsWith("2.");

        TargetBean target = rootBean.getTarget();
        assert target.getEndpoint().equals(getCfApiEndpointValue());
        assert target.getOrg().equals(getCfOrganizationValue());
        assert target.getSpace().equals(getCfSpaceValue());

        SpecBean spec = rootBean.getSpec();
        // TODO: check if we can put in an absolute value once we use "throw-away spaces" created per test run
        assert spec.getSpaceDevelopers().size() > 0;
    }

    private ConfigBean loadConfigBeanFromStdoutAndAssertItsValidity(String stdoutContent) {
        ConfigBean rootBean = YamlMapper.loadBeanFromString(stdoutContent, ConfigBean.class);
        assertRootBeanIsValid(rootBean);
        return rootBean;
    }

    @Test
    public void testEmptySpace() {
        ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("get");

        RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args);

        String outContent = runResult.getStreamContents().getStdoutContent();
        assert outContent.length() > 0;

        // parse YAML from stdout and check it for validity
        ConfigBean rootBean = loadConfigBeanFromStdoutAndAssertItsValidity(outContent);

        SpecBean spec = rootBean.getSpec();
        assert spec.getApps() == null;
        assert spec.getServices() == null;

        // TODO: check log contents
        String errContent = runResult.getStreamContents().getStderrContent();
        assert errContent.length() > 0;

        assert runResult.getExitCode() == 0;
    }

    @Test
    public void testSpaceWithOneService() {
        ServiceBean service = new ServiceBean();
        service.setService("elephantsql");
        service.setPlan("turtle");
        spaceConfigurator.addDesiredService("test-service", service);
        spaceConfigurator.configure();

        ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("get");

        RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args);
        String stderrContents = runResult.getStreamContents().getStderrContent();
        String stdoutContents = runResult.getStreamContents().getStdoutContent();

        assert stderrContents.length() > 0;
        assert stdoutContents.length() > 0;

        assert runResult.getExitCode() == 0;
    }

    @Test
    public void testSpaceWithOneApplication() {
        ApplicationBean application = new ApplicationBean();

        ApplicationManifestBean manifest = new ApplicationManifestBean();
        // just some random web service
        manifest.setDockerImage("kennethreitz/httpbin");

        application.setManifest(manifest);

        // note: the application name must be unique, it'll be used as domain
        spaceConfigurator.addDesiredApplication("cfcli-test-app", application);
        spaceConfigurator.configure();

        ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("get");

        RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args);
        String stderrContents = runResult.getStreamContents().getStderrContent();
        String stdoutContents = runResult.getStreamContents().getStdoutContent();

        assert stderrContents.length() > 0;
        assert stdoutContents.length() > 0;

        assert runResult.getExitCode() == 0;
    }

}