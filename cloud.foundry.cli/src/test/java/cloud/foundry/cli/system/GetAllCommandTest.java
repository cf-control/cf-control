package cloud.foundry.cli.system;

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

    @Test
    public void testEmptySpace() {
        ArgumentsBuilder args = new ArgumentsBuilder()
                .addArgument("get");

        RunResult runResult = runBaseControllerWithCredentialsFromEnvironment(args);

        String outContent = runResult.getStreamContents().getStdoutContent();
        assert outContent.length() > 0;

        String errContent = runResult.getStreamContents().getStderrContent();
        assert errContent.length() > 0;

        assert runResult.getExitCode() == 0;
    }

}
