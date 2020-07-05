package cloud.foundry.cli.services;

import static picocli.CommandLine.usage;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;

import java.util.concurrent.Callable;

/**
 * TODO
 */
@Command(name = "dump", header = "%n@|green TODO|@",
        mixinStandardHelpOptions = true, subcommands = {
        DumpController.DumpAllCommand.class,
        DumpController.DumpServicesCommand.class,
        DumpController.DumpApplicationsCommand.class,
        DumpController.DumpSpaceDevelopersCommand.class
})
public class DumpController implements Callable<Integer> {

    // FIXME this field is actually not necessary, but needed due to the argument extension mechanism in the class
    // CfArgumentsCreator...
    @Mixin
    private static LoginCommandOptions loginOptions;

    @Override
    public Integer call() {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "all", description = "TODO")
    static class DumpAllCommand extends DumpCommandBase<ConfigBean> {

        /**
         * TODO
         */
        protected DumpAllCommand() {
            super(ConfigBean.class, DumpAllCommand.class);
        }
    }

    @Command(name = "services", description = "TODO")
    static class DumpServicesCommand extends DumpCommandBase<SpecBean> {

        /**
         * TODO
         */
        protected DumpServicesCommand() {
            super(SpecBean.class, DumpServicesCommand.class);
        }
    }

    @Command(name = "applications", description = "TODO")
    static class DumpApplicationsCommand extends DumpCommandBase<SpecBean> {

        /**
         * TODO
         */
        protected DumpApplicationsCommand() {
            super(SpecBean.class, DumpApplicationsCommand.class);
        }
    }

    @Command(name = "space-developers", description = "TODO")
    static class DumpSpaceDevelopersCommand extends DumpCommandBase<SpecBean> {

        /**
         * TODO
         */
        protected DumpSpaceDevelopersCommand() {
            super(SpecBean.class, DumpSpaceDevelopersCommand.class);
        }
    }
}