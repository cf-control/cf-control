package cloud.foundry.cli.getservice;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.getservice.logic.ServiceInstanceSummaryBean;
import cloud.foundry.cli.getservice.logic.SpaceDevelopersProvider;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
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
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator
                    .createCfOperations(commandOptions);

            SpaceDevelopersProvider provider = new SpaceDevelopersProvider(cfOperations);
            String  spaceDevelopers =  provider.getSpaceDevelopers();
            System.out.println(spaceDevelopers);
        }
    }

    @Command(name = "get-services", description = "List all applications in the target space")
    static class GetServicesCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            // FIXME
            System.out.println("SOME DUMMY SERVICES");

            // RUFE SERVICE AUF
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator
                    .createCfOperations(commandOptions);
            List<ServiceInstanceSummary> services = cfOperations.services()
                    .listInstances().collectList().block();
            for (ServiceInstanceSummary serviceInstanceSummary : services) {
                // do not dump tags into the document
                ServiceInstanceSummaryBean serviceInstance = new ServiceInstanceSummaryBean(serviceInstanceSummary);
                DumperOptions options = new DumperOptions();
                options.setTags(new HashMap<String, String>()); // do not dump tags into the document
                Yaml yaml = new Yaml(options);
                String yamlDocument = yaml.dumpAsMap(serviceInstance);
                System.out.println(yamlDocument);
            }
        }
    }

    @Command(name = "get-applications", description = "List all applications in the target space")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        GetControllerCommandOptions commandOptions;

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
        CommandLine.run(new GetServicesCommand(), System.err, args);
    }
}
