package cloud.foundry.cli.services;

import picocli.CommandLine;

@CommandLine.Command(name = "base", header = "%n@|green Base-Controller|@", subcommands = {
        CreateController.class,
        GetController.class})
public class BaseController implements Runnable {

    @Override
    public void run() {
        // this code is executed if the user runs the create command without specifying
        // any sub-command
    }

}
