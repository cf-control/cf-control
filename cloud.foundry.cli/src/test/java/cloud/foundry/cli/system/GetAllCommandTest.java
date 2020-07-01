package cloud.foundry.cli.system;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.*;
import cloud.foundry.cli.system.util.ArgumentsBuilder;
import cloud.foundry.cli.system.util.RunResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link cloud.foundry.cli.services.GetController}, specifically the default get command which fetches all
 * the information and outputs them in the same format we expect for the input.
 */
public class GetAllCommandTest extends SystemTestBase {

    @BeforeEach
    private void cleanUpSpaceBefore() {
        getSpaceConfigurator().clear();
    }

    @AfterAll
    private static void cleanUpSpaceAfterwards() {
        getSpaceConfigurator().clear();
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
        String serviceName = getSpaceConfigurator().requestCreationOfService(service);
        getSpaceConfigurator().createRequestedEntities();

        ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("get");

        RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args);

        String outContent = runResult.getStreamContents().getStdoutContent();
        assert outContent.length() > 0;

        // parse YAML from stdout and check it for validity
        ConfigBean rootBean = loadConfigBeanFromStdoutAndAssertItsValidity(outContent);

        SpecBean spec = rootBean.getSpec();
        assert spec.getApps() == null;

        assert spec.getServices().size() == 1;
        ServiceBean parsedService = spec.getServices().get(serviceName);
        assert parsedService.getService().equals(service.getService());
        assert parsedService.getPlan().equals(service.getPlan());

        // TODO: check log contents
        String errContent = runResult.getStreamContents().getStderrContent();
        assert errContent.length() > 0;

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
        String appName = getSpaceConfigurator().requestCreationOfApplication(application);
        getSpaceConfigurator().createRequestedEntities();

        ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("get");

        RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args);

        String outContent = runResult.getStreamContents().getStdoutContent();
        assert outContent.length() > 0;

        // parse YAML from stdout and check it for validity
        ConfigBean rootBean = loadConfigBeanFromStdoutAndAssertItsValidity(outContent);

        SpecBean spec = rootBean.getSpec();
        assert spec.getServices() == null;

        assert spec.getApps().size() == 1;
        ApplicationBean parsedApplication = spec.getApps().get(appName);
        assert parsedApplication.getPath() == null;
        ApplicationManifestBean parsedManifest = parsedApplication.getManifest();
        assert parsedManifest.getDockerImage().equals(manifest.getDockerImage());

        // TODO: check log contents
        String errContent = runResult.getStreamContents().getStderrContent();
        assert errContent.length() > 0;

        assert runResult.getExitCode() == 0;
    }

}
