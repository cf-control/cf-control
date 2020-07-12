package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;

import cloud.foundry.cli.services.BaseController;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Test for {@link CfArgumentsCreator}
 */
public class CfArgumentsCreatorTest {

    @Test
    public void determineCommandLineForGetCommand_withAllMissingOptions() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
            cli,
            new String[] { "get" },
            null);

        // then
        assertThat(result,
            arrayContaining("get", "-a", "api.run.pivotal.io", "-o", "cloud.foundry.cli", "-s", "development"));
    }

    @Test
    public void determineCommandLineForGetCommand_withOneMissingOption() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
            cli,
            new String[] { "get", "-s", "development", "-a", "api.run.pivotal.io" },
            null);

        // then
        assertThat(result,
            arrayContaining("get", "-s", "development", "-a", "api.run.pivotal.io", "-o", "cloud.foundry.cli"));
    }

    @Test
    public void determineCommandLineForGetCommand_withOutMissingOptions() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
            cli,
            new String[] { "get", "-s", "development", "-a", "api.run.pivotal.io", "-o", "cloud.foundry.cli" },
            null);

        // then
        assertThat(result,
            arrayContaining("get", "-s", "development", "-a", "api.run.pivotal.io", "-o", "cloud.foundry.cli"));
    }

    @Test
    public void determineCommandLineForDiffApplyCommand_withAllMissingOptions() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);
        String configFilePath = "./src/test/resources/basic/configBean.yml";

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
            cli,
            new String[] { "diff", "all", "-y", configFilePath },
            null);

        // then
        assertThat(result,
            arrayContaining("diff", "all", "-y", configFilePath, "-a", "api.run.pivotal.io", 
                            "-o", "cloud.foundry.cli", "-s", "development"));
    }

    @Test
    public void determineCommandLineForDiffApplyCommand_withOneMissingOption() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);
        String configFilePath = "./src/test/resources/basic/configBean.yml";

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
            cli,
            new String[] { "diff", "all", "-y", configFilePath, "-a", "api.run.pivotal.io", "-o","cloud.foundry.cli"},
            null);

        // then
        assertThat(result, 
            arrayContaining("diff", "all", "-y", configFilePath, "-a",
                            "api.run.pivotal.io", "-o","cloud.foundry.cli", "-s", "development"));
    }

    @Test
    public void determineCommandLineForDiffApplyCommand_withoutMissingOptions() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);
        String configFilePath = "./src/test/resources/basic/configBean.yml";

        String[] args = { "diff", "all", "-y", configFilePath, "-a", "api.run.pivotal.io",
            "-o", "cloud.foundry.cli", "-s", "development" };

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(cli, args, null);

        // then
        assertThat(result, arrayContaining(args));
    }

}
