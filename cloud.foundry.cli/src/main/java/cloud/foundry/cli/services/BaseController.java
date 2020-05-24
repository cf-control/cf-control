package cloud.foundry.cli.services;

import picocli.CommandLine;

/**
 * This class works as the entry point for the command line application.
 * Based on this entrypoint you can call subcommands depending on your use case.
 * Called in {@link cloud.foundry.cli.Main#main(String[])}
 */
@CommandLine.Command(name = "cf-control", header = "%n@|green Welcome to cf-control|@",
        description = "This program helps in configuring your cf instance.",
        mixinStandardHelpOptions = true,
        version = "1.0",
        subcommands = {
        CreateController.class,
        GetController.class})
public class BaseController implements Runnable {

    @Override
    public void run() {
        // this code is executed if the user just runs the app
        CommandLine.usage(this, System.out);
        return;
    }

}
