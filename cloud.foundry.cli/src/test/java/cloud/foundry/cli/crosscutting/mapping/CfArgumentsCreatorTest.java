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
    public void determineCommandLineShouldExtendTheCommandLineArgumentsIfAllRelevantOptionsAreMissing() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
                cli,
                new String[]{"diff", "-y", "somePath"},
                CommandLine.ParseResult.builder(CommandLine.Model.CommandSpec.create()).build());

        // then
        assertThat(result, arrayContaining("diff", "-y", "somePath", "-a",
                "api.run.pivotal.io", "-o", "cloud.foundry.cli", "-s", "development"));
    }

    @Test
    public void determineCommandLineShouldExtendTheCommandLineArgumentsIfOneRelevantOptionIsMissing() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(
                cli,
                new String[]{"diff", "-s", "development", "-y", "somePath"},
                CommandLine.ParseResult.builder(CommandLine.Model.CommandSpec.create()).build());

        // then
        assertThat(result, arrayContaining("diff", "-s", "development", "-y", "somePath", "-a",
                "api.run.pivotal.io", "-o", "cloud.foundry.cli"));
    }

    @Test
    public void determineCommandLineShouldNotExtendTheCommandLineArgumentsIfAllRelevantOptionsArePassed() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        String[] args = {"diff", "-y", "somePath", "-a", "api.run.pivotal.io",
                "-o", "cloud.foundry.cli", "-s", "development"};

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(cli, args, null);

        // then
        assertThat(result, arrayContaining(args));
    }

}

