package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.operations.AllInformationOperations;
import cloud.foundry.cli.operations.ApplicationOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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
public class GetController implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        // by default, return all information
        // this is a convenient shortcut
        return (new GetController.GetAllInformation()).call();
    }

    @Command(name = "space-developers",
            description = "List all space developers in the target space.",
            mixinStandardHelpOptions = true
    )
    static class GetSpaceDevelopersCommand implements Callable<Integer> {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
            Mono<List<String>> spaceDevelopers = spaceDevelopersOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopers.block()));
            return 0;
        }
    }

    @Command(name = "services", description = "List all services in the target space.")
    static class GetServicesCommand implements Callable<Integer> {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            Mono<Map<String,ServiceBean>> services = servicesOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(services.block()));
            return 0;
        }
    }

    @Command(name = "applications", description = "List all applications in the target space.")
    static class GetApplicationsCommand implements Callable<Integer> {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationOperations applicationOperations = new ApplicationOperations(cfOperations);
            Mono<Map<String, ApplicationBean>> applications = applicationOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(applications.block()));
            return 0;
        }
    }

    @Command(name = "all", description = "show all information in the target space")
    static class GetAllInformation implements Callable<Integer> {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            AllInformationOperations allInformationOperations = new AllInformationOperations(cfOperations);
            ConfigBean allInformation = allInformationOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(allInformation));
            return 0;
        }
    }
}
