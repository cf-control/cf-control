package cloud.foundry.cli.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.*;
import cloud.foundry.cli.system.util.ArgumentsBuilder;
import cloud.foundry.cli.system.util.RunResult;
import cloud.foundry.cli.system.util.SpaceManager;
import org.junit.jupiter.api.Test;


/**
 * Test for {@link cloud.foundry.cli.services.GetController}, specifically the default get command which fetches all
 * the information and outputs them in the same format we expect for the input.
 */
public class GetCommandTest extends SystemTestBase {

    private void assertRootBeanIsValid(ConfigBean rootBean, SpaceManager spaceManager) {
        assert rootBean.getApiVersion().startsWith("2.");

        TargetBean target = rootBean.getTarget();
        assert target.getEndpoint().equals(spaceManager.getCfApiEndpoint());
        assert target.getOrg().equals(spaceManager.getCfOrganization());
        assert target.getSpace().equals(spaceManager.getSpaceName());

        SpecBean spec = rootBean.getSpec();
        // should contain only our current user, who created the space
        assert spec.getSpaceDevelopers().size() == 1;
        assert spec.getSpaceDevelopers().contains(spaceManager.getCfUsername());
    }

    private ConfigBean loadConfigBeanFromStdoutAndAssertItsValidity(String stdoutContent, SpaceManager spaceManager) {
        ConfigBean rootBean = YamlMapper.loadBeanFromString(stdoutContent, ConfigBean.class);
        assertRootBeanIsValid(rootBean, spaceManager);
        return rootBean;
    }

    @Test
    public void testEmptySpace() {
        try (SpaceManager spaceManager = createSpaceWithRandomName()) {
            ArgumentsBuilder args = new ArgumentsBuilder()
                    .addArgument("get");

            RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);

            String outContent = runResult.getStreamContents().getStdoutContent();
            assert outContent.length() > 0;

            // parse YAML from stdout and check it for validity
            ConfigBean rootBean = loadConfigBeanFromStdoutAndAssertItsValidity(outContent, spaceManager);

            SpecBean spec = rootBean.getSpec();
            assert spec.getApps() == null;
            assert spec.getServices() == null;
            assert spec.getSpaceDevelopers().size() > 0;

            // there's no log messages >= INFO we could test against, only from setup/teardown, so we just check
            // there's *some* contents
            String errContent = runResult.getStreamContents().getStderrContent();
            assert errContent.length() > 0;

            assert runResult.getExitCode() == 0;
        }
    }

    @Test
    public void testSpaceWithOneService() {
        try (SpaceManager spaceManager = createSpaceWithRandomName()) {
            ServiceBean service = new ServiceBean();
            service.setService("elephantsql");
            service.setPlan("turtle");
            String serviceName = spaceManager.requestCreationOfService(service);
            spaceManager.createRequestedEntities();

            ArgumentsBuilder args = new ArgumentsBuilder()
                    .addArgument("get");

            RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);

            String outContent = runResult.getStreamContents().getStdoutContent();
            assert outContent.length() > 0;

            // parse YAML from stdout and check it for validity
            ConfigBean rootBean = loadConfigBeanFromStdoutAndAssertItsValidity(outContent, spaceManager);

            SpecBean spec = rootBean.getSpec();
            assert spec.getApps() == null;

            assert spec.getServices().size() == 1;
            ServiceBean parsedService = spec.getServices().get(serviceName);
            assert parsedService.getService().equals(service.getService());
            assert parsedService.getPlan().equals(service.getPlan());
            assert parsedService.getTags() == null;

            // there's no log messages >= INFO we could test against, only from setup/teardown, so we just check
            // there's *some* contents
            String errContent = runResult.getStreamContents().getStderrContent();
            assert errContent.length() > 0;

            assert runResult.getExitCode() == 0;
        }
    }

    @Test
    public void testSpaceWithOneApplication() {
        try (SpaceManager spaceManager = createSpaceWithRandomName()) {
            ApplicationBean application = new ApplicationBean();
            application.setPath("src/test/resources/system/demo-python-app");

            ApplicationManifestBean manifest = new ApplicationManifestBean();
            application.setManifest(manifest);
            manifest.setBuildpack("https://github.com/cloudfoundry/python-buildpack.git");

            // note: the application name must be unique, it'll be used as domain
            String appName = spaceManager.requestCreationOfApplication(application);
            spaceManager.createRequestedEntities();

            ArgumentsBuilder args = new ArgumentsBuilder()
                    .addArgument("get");

            RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args, spaceManager);

            String outContent = runResult.getStreamContents().getStdoutContent();
            assertThat(outContent.length(), is(greaterThan(0)));

            // parse YAML from stdout and check it for validity
            ConfigBean rootBean = loadConfigBeanFromStdoutAndAssertItsValidity(outContent, spaceManager);

            SpecBean spec = rootBean.getSpec();
            assertThat(spec.getServices(),  is(nullValue()));

            assertThat(spec.getApps().size(), is(1));
            ApplicationBean parsedApplication = spec.getApps().get(appName);
            assertThat(parsedApplication.getPath(), is("src/test/resources/system/demo-python-app"));

            String buildpack = parsedApplication.getManifest().getBuildpack();
            assertThat(buildpack, is("https://github.com/cloudfoundry/python-buildpack.git"));
            // there's no log messages >= INFO we could test against, only from setup/teardown, so we just check
            // there's *some* contents
            String errContent = runResult.getStreamContents().getStderrContent();
            assertThat(errContent.length(), is(greaterThan(0)));

            assertThat(runResult.getExitCode(), is(0));
        }
    }

}
