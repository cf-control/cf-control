package cloud.foundry.cli;

import cloud.foundry.cli.services.BaseController;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BaseController()).execute(args);
        System.exit(exitCode);
    }
}
