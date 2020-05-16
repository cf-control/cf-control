package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceInstanceSummaryBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.YamlUtils;
import cloud.foundry.cli.operations.ApplicationOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.List;

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

    }

    @Command(name = "space-developers",
            description = "List all space developers in the target space")
    static class GetSpaceDevelopersCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
            List<SpaceDevelopersBean> spaceDevelopers = spaceDevelopersOperations.getAll();

            System.out.println(YamlUtils.createDefaultYamlParser().dump(spaceDevelopers));
        }
    }

    @Command(name = "services", description = "List all applications in the target space")
    static class GetServicesCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            List<ServiceInstanceSummaryBean> services = servicesOperations.getAll();

            System.out.println(YamlUtils.createDefaultYamlParser().dump(services));
        }
    }

    @Command(name = "applications", description = "List all applications in the target space")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);
            ApplicationOperations applicationOperations = new ApplicationOperations(cfOperations);
            List<ApplicationBean> applications = applicationOperations.getAll();

            System.out.println(YamlUtils.createDefaultYamlParser().dump(applications));
        }
    }

    public static void main(String... args)
    {
        CommandLine.run(new GetController(), System.err, args);
    }
}
