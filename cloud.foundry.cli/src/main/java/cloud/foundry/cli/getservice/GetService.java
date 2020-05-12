package cloud.foundry.cli.getservice;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.getservice.logic.SpaceDevelopersProvider;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;


@Command(name = "Get-Service",
        header = "%n@|green Get-Service|@",
        subcommands = {
                GetService.GetServicesCommand.class,
                GetService.GetSpaceDevelopersCommand.class,
                GetService.GetApplicationsCommand.class})
public class GetService implements Runnable {

    @Override
    public void run() {

    }

    @Command(name = "getSpaceDevelopers", description = "List all space developers")
    static class GetSpaceDevelopersCommand implements Runnable {
        @Mixin
        GetServiceCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator
                    .createCfOperations(commandOptions);

            SpaceDevelopersProvider provider = new SpaceDevelopersProvider(cfOperations);
            String  spaceDevs =  provider.getSpaceDevelopers();
            System.out.println(spaceDevs);
           
        }
    }

    @Command(name = "getServices", description = "List all applications in the target space")
    static class GetServicesCommand implements Runnable {
        @Mixin
        GetServiceCommandOptions commandOptions;

        @Override
        public void run() {
            // FIXME
            System.out.println("SOME DUMMY SERVICES");

            // RUFE SERVICE AUF
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator
                    .createCfOperations(commandOptions);
        }
    }

    @Command(name = "getApplications", description = "List all applications in the target space")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        GetServiceCommandOptions commandOptions;

        @Override
        public void run() {
            // FIXME
            System.out.println("SOME DUMMY APPLICATIONS");
            // RUFE SERVICE AUF
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator
                    .createCfOperations(commandOptions);
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new GetService()).execute(args);
        System.exit(exitCode);
    }
}
