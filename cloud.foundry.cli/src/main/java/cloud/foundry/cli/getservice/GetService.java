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

import java.util.HashMap;
import java.util.List;


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
            // FIXME
            System.out.println("SOME DUMMY SPACE DEVELOPERS");
            // RUFE SERVICE AUF
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator
                    .createCfOperations(commandOptions);

            SpaceDevelopersProvider provider = new SpaceDevelopersProvider();
            provider.machWas(cfOperations);
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
            List<ServiceInstanceSummary> services = cfOperations.services().listInstances().collectSortedList().block();
            for (ServiceInstanceSummary serviceInstanceSummary : services) {
                System.out.println("Service: " +  serviceInstanceSummary.toString());
            }
            System.out.println("Yaml representation");
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
