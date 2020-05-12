package cloud.foundry.cli.getservice;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.getservice.logic.GetService;
import cloud.foundry.cli.getservice.logic.SpaceDevelopersProvider;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Controller for the Get-Commands
 */
@Command(name = "Get-Controller",
        header = "%n@|green Get-Controller|@",
        subcommands = {
                GetController.GetServicesCommand.class,
                GetController.GetSpaceDevelopersCommand.class,
                GetController.GetApplicationsCommand.class})
public class GetController implements Runnable {

    @Override
    public void run() {

    }

    @Command(name = "get-space-developers",
            description = "List all space developers in the target space")
    static class GetSpaceDevelopersCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);

            SpaceDevelopersProvider provider = new SpaceDevelopersProvider(cfOperations);

            String spaceDevelopers =  provider.getSpaceDevelopers();

            System.out.println(spaceDevelopers);
        }
    }

    @Command(name = "get-services", description = "List all applications in the target space")
    static class GetServicesCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);

            GetService getService = new GetService(cfOperations);

            String services = getService.getServices();

            if (services == null) {
                System.out.println("No services.");
            }

            System.out.println(services);
        }
    }

    @Command(name = "get-applications", description = "List all applications in the target space")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);
            // TODO
        }
    }

    public static void main(String... args) {
        CommandLine.run(new GetServicesCommand(), System.err, args);
    }
}
