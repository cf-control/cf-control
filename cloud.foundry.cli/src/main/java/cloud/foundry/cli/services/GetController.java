package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.CredentialsConfigReader;
import cloud.foundry.cli.operations.GetService;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Controller for the Get-Commands
 */
@Command(name = "get",
        header = "%n@|green Get-Controller|@",
        subcommands = {
                GetController.GetServicesCommand.class,
                GetController.GetSpaceDevelopersCommand.class,
                GetController.GetApplicationsCommand.class})
public class GetController implements Runnable {

    @Override
    public void run() {
        //getSpaceDevelopers
        //getServices
        //getApp
        // construct yaml
    }

    @Command(name = "space-developers",
            description = "List all space developers in the target space")
    static class GetSpaceDevelopersCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);

            SpaceDevelopersOperations provider = new SpaceDevelopersOperations(cfOperations);

            String spaceDevelopers =(String) provider.get();

            System.out.println(spaceDevelopers);
        }
    }

    @Command(name = "services", description = "List all applications in the target space")
    static class GetServicesCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);

            GetService getService = new GetService(cfOperations);

            String services = getService.getServices();

            System.out.println(services);
        }
    }

    @Command(name = "applications", description = "List all applications in the target space")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);
            GetService getService = new GetService(cfOperations);

            String applications = getService.getApplications();

            System.out.println(applications);
        }
    }

    public static void main(String... args)
    {
        CommandLine.run(new GetApplicationsCommand(), System.err, args);
    }
}
