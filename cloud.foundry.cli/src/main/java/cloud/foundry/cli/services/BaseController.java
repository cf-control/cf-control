package cloud.foundry.cli.services;

import picocli.CommandLine;

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
