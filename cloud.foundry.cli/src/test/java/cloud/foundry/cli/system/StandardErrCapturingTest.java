package cloud.foundry.cli.system;

import org.junit.jupiter.api.Test;


public class StandardErrCapturingTest extends SystemTestBase {

    @Test
    public void testSystemerrCapturing() {
        String[] arguments = new ArgumentsBuilder()
                // let's be as verbose as possible
                .addArgument("-d")

                // first random command; doesn't really matter, it won't work anyway
                .addArgument("get")
                // need to provide *all* required CLI options, otherwise the tool will error out even _before_ the log
                // file parameter could be evaluated
                .addOption("-a", "test")
                .addOption("-o", "test")
                .addOption("-s", "test")
                .build();

        RunResult runResult = runBaseControllerWithArgs(arguments);

        assert runResult.getStreamContents().getStderrContent().length() > 0 ;

        assert runResult.getExitCode() == 1;
    }
}
