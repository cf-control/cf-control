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
        String[] result = CfArgumentsCreator.determineCommandLine(cli,
                new String[]{"diff", "services", "-y", "somePath"});

        // then
        assertThat(result, arrayContaining("diff", "services", "-y", "somePath", "-a",
                "api.run.pivotal.io", "-o", "cloud.foundry.cli", "-s", "development"));
    }

    @Test
    public void determineCommandLineShouldExtendTheCommandLineArgumentsIfOneRelevantOptionIsMissing() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(cli,
                new String[]{"diff", "services", "-s", "development", "-y", "somePath"});

        // then
        assertThat(result, arrayContaining("diff", "services", "-s", "development", "-y", "somePath", "-a",
                "api.run.pivotal.io", "-o", "cloud.foundry.cli"));
    }

    @Test
    public void determineCommandLineShouldNotExtendTheCommandLineArgumentsIfAllRelevantOptionsArePassed() {
        // given
        BaseController controller = new BaseController();
        CommandLine cli = new CommandLine(controller);

        String[] args = {"diff", "services", "-y", "somePath", "-a", "api.run.pivotal.io",
                "-o", "cloud.foundry.cli", "-s", "development"};

        // when
        String[] result = CfArgumentsCreator.determineCommandLine(cli, args);

        // then
        assertThat(result, arrayContaining(args));
    }

}
