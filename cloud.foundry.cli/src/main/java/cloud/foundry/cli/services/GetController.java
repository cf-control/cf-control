package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.logic.GetLogic;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import reactor.core.publisher.Mono;

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
                Mono<List<String>> spaceDevelopers = spaceDevelopersOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopers.block()));
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
                Mono<Map<String,ServiceBean>> services = servicesOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(services.block()));
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
                ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
                Mono<Map<String, ApplicationBean>> applications = applicationsOperations.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(applications.block()));
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
                GetLogic getLogic = new GetLogic(cfOperations);
                ConfigBean allInformation = getLogic.getAll();

                System.out.println(YamlCreator.createDefaultYamlProcessor().dump(allInformation));
            } catch (Exception e) {
                Log.exception(e, "Unexpected error occurred");
            }
        }
    }
}
