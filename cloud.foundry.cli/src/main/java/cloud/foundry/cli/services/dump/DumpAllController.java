package cloud.foundry.cli.services.dump;

import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.services.LoginCommandOptions;
import picocli.CommandLine;

/**
 * TODO
 */
@CommandLine.Command(name = "dump", header = "%n@|green TODO|@",
        mixinStandardHelpOptions = true, subcommands = {
        DumpServicesCommand.class,
        DumpApplicationsCommand.class,
        DumpSpaceDevelopersCommand.class
})
public class DumpAllController extends AbstractDumpCommand<ConfigBean> {

    // this field is actually not necessary, but needed due to the argument extension mechanism in the class
    // CfArgumentsCreator...
    @CommandLine.Mixin
    LoginCommandOptions loginOptions;

    /**
     * TODO
     */
    protected <C extends AbstractDumpCommand> DumpAllController() {
        super(ConfigBean.class, DumpAllController.class);
    }
}
