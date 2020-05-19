package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.operations.AllInformationOperations;
import cloud.foundry.cli.operations.ApplicationOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class realizes the functionality that is needed for the get commands. They provide various information about a
 * cloud foundry instance.
 */
@Command(name = "get",
        header = "%n@|green Get-Controller|@",
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
            description = "List all space developers in the target space")
    static class GetSpaceDevelopersCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
            SpaceDevelopersBean spaceDevelopers = spaceDevelopersOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(spaceDevelopers));
        }
    }

    @Command(name = "services", description = "List all services in the target space")
    static class GetServicesCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            List<ServiceBean> services = servicesOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(services));
        }
    }

    @Command(name = "applications", description = "List all applications in the target space")
    static class GetApplicationsCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationOperations applicationOperations = new ApplicationOperations(cfOperations);
            List<ApplicationBean> applications = applicationOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(applications));
        }
    }

    @Command(name = "all", description = "show all information in the target space")
    static class GetAllInformation implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            AllInformationOperations allInformationOperations = new AllInformationOperations(cfOperations);
            GetAllBean allInformation = allInformationOperations.getAll();

            System.out.println(YamlCreator.createDefaultYamlProcessor().dump(allInformation));
        }
    }
}
