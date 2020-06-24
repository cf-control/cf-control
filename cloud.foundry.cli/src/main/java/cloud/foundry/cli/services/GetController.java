package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
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

        SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
        ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

        ConfigBean allInformation = getLogic.getAll(
                spaceDevelopersOperations, servicesOperations, applicationsOperations);
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

            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
            List<String> spaceDevelopers = getLogic.getSpaceDevelopers(spaceDevelopersOperations);
            System.out.println(YamlMapper.dump(spaceDevelopers));

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

            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            Map<String, ServiceBean> services = getLogic.getServices(servicesOperations);
            System.out.println(YamlMapper.dump(services));

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

            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
            Map<String, ApplicationBean> applications = getLogic.getApplications(applicationsOperations);
            System.out.println(YamlMapper.dump(applications));

            return 0;
        }
    }

}