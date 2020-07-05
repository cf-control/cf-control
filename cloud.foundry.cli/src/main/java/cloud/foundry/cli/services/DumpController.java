package cloud.foundry.cli.services;

import static picocli.CommandLine.usage;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.mapping.RefResolver;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the dump commands.
 * It defines command classes that take care of the command execution. They are derived from the {@link DumpCommandBase}
 * class that contains common behaviour of all the subcommands.
 */
@Command(name = "dump", header = "%n@|green Resolve " + RefResolver.REF_KEY + "-occurrences in a configuration file" +
        " and print the result.|@",
        mixinStandardHelpOptions = true, subcommands = {
        DumpController.DumpAllCommand.class,
        DumpController.DumpServicesCommand.class,
        DumpController.DumpApplicationsCommand.class,
        DumpController.DumpSpaceDevelopersCommand.class
})
public class DumpController implements Callable<Integer> {

    // FIXME this field is actually not necessary, but needed due to the argument extension mechanism in the class
    // CfArgumentsCreator... its contents are simply ignored by the dump command
    @Mixin
    private static LoginCommandOptions loginOptions;

    @Override
    public Integer call() {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "all", description = "Resolve " + RefResolver.REF_KEY + "-occurrences in an entire " +
            "configuration file and print the result.")
    static class DumpAllCommand extends DumpCommandBase<ConfigBean> {

        /**
         * Constructor that specifies the bean type and the command class for {@link DumpCommandBase}
         */
        protected DumpAllCommand() {
            super(ConfigBean.class, DumpAllCommand.class);
        }
    }

    @Command(name = "services", description = "Resolve " + RefResolver.REF_KEY + "-occurrences in a service " +
            "configuration file and print the result.")
    static class DumpServicesCommand extends DumpCommandBase<SpecBean> {

        /**
         * Constructor that specifies the bean type and the command class for {@link DumpCommandBase}
         */
        protected DumpServicesCommand() {
            super(SpecBean.class, DumpServicesCommand.class);
        }
    }

    @Command(name = "applications", description = "Resolve " + RefResolver.REF_KEY + "-occurrences in an application " +
            "configuration file and print the result.")
    static class DumpApplicationsCommand extends DumpCommandBase<SpecBean> {

        /**
         * Constructor that specifies the bean type and the command class for {@link DumpCommandBase}
         */
        protected DumpApplicationsCommand() {
            super(SpecBean.class, DumpApplicationsCommand.class);
        }
    }

    @Command(name = "space-developers", description = "Resolve " + RefResolver.REF_KEY + "-occurrences in a " +
            "space-developers configuration file and print the result.")
    static class DumpSpaceDevelopersCommand extends DumpCommandBase<SpecBean> {

        /**
         * Constructor that specifies the bean type and the command class for {@link DumpCommandBase}
         */
        protected DumpSpaceDevelopersCommand() {
            super(SpecBean.class, DumpSpaceDevelopersCommand.class);
        }
    }
}