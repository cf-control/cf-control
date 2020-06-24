package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.logic.GetLogic;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the get commands.
 * They provide various information about a cloud foundry instance.
 */
@Command(name = "get", header = "%n@|green Get the current configuration of your cf instance.|@",
        mixinStandardHelpOptions = true, subcommands = {
        GetController.GetServicesCommand.class,
        GetController.GetSpaceDevelopersCommand.class,
        GetController.GetApplicationsCommand.class
})
public class GetController implements Callable<Integer> {

    @Mixin
    private static LoginCommandOptions loginOptions;

    @Override
    public Integer call() throws Exception {
        DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
        GetLogic getLogic = new GetLogic(cfOperations);
        Log.info("Fetching all information for target space...");

        ConfigBean allInformation = getLogic.getAll();
        System.out.println(YamlMapper.dump(allInformation));
        return 0;
    }

    @Command(name = "space-developers",
            description = "List all space developers in the target space.",
            mixinStandardHelpOptions = true)
    static class GetSpaceDevelopersCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            GetLogic getLogic = new GetLogic(cfOperations);
            Log.info("Fetching information of space developers...");

            Mono<List<String>> spaceDevelopers = getLogic.getSpaceDevelopers();
            System.out.println(YamlMapper.dump(spaceDevelopers.block()));

            return 0;
        }
    }

    @Command(name = "services", description = "List all services in the target space.", mixinStandardHelpOptions = true)
    static class GetServicesCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            GetLogic getLogic = new GetLogic(cfOperations);
            Log.info("Fetching information for services...");

            Mono<Map<String, ServiceBean>> services = getLogic.getServices();
            System.out.println(YamlMapper.dump(services.block()));

            return 0;
        }
    }

    @Command(name = "applications",
            description = "List all applications in the target space.",
            mixinStandardHelpOptions = true)
    static class GetApplicationsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            GetLogic getLogic = new GetLogic(cfOperations);
            Log.info("Fetching information for apps...");

            Mono<Map<String, ApplicationBean>> applications = getLogic.getApplications();
            System.out.println(YamlMapper.dump(applications.block()));

            return 0;
        }
    }

}