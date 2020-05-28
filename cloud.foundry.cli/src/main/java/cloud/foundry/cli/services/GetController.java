package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.operations.AllInformationOperations;
import cloud.foundry.cli.operations.ApplicationOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.List;
import java.util.Map;

/**
 * This class realizes the functionality that is needed for the get commands. They provide various information about a
 * cloud foundry instance.
 */
@Command(name = "get",
        header = "%n@|green Get the current configuration of your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {
                GetController.GetServicesCommand.class,
                GetController.GetSpaceDevelopersCommand.class,
                GetController.GetApplicationsCommand.class,
                GetController.GetAllInformation.class})
public class GetController implements Runnable {

    @Override
    public void run() {
        // this code is executed if the user runs the get command without specifying any sub-command
        CommandLine.usage(this, System.out);
        return;
    }

    @Command(name = "space-developers",
            description = "List all space developers in the target space.",
            mixinStandardHelpOptions = true
    )
    static class GetSpaceDevelopersCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            try {
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
                SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
                SpaceDevelopersBean spaceDevelopers = spaceDevelopersOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopers));
            } catch (Exception e) {
                Log.exception(e, "Unexpected error occurred");
            }
        }
    }

    @Command(name = "services", description = "List all services in the target space.")
    static class GetServicesCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            try {
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
                ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
                Map<String,ServiceBean> services = servicesOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(services));
            } catch (Exception e) {
                Log.exception(e, "Unexpected error occurred");
            }
        }
    }

    @Command(name = "applications", description = "List all applications in the target space.")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            try {
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
                ApplicationOperations applicationOperations = new ApplicationOperations(cfOperations);
                List<ApplicationBean> applications = applicationOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(applications));
            } catch (Exception e) {
                Log.exception(e, "Unexpected error occurred");
            }
        }
    }

    @Command(name = "all", description = "show all information in the target space")
    static class GetAllInformation implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            try {
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
                AllInformationOperations allInformationOperations = new AllInformationOperations(cfOperations);
                GetAllBean allInformation = allInformationOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(allInformation));
            } catch (Exception e) {
                Log.exception(e, "Unexpected error occurred");
            }
        }
    }
}
