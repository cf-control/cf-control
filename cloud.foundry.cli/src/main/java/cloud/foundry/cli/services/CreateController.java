package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.IOException;
import java.util.LinkedList;

/**
 * This class realizes the functionality that is needed for the create commands.
 */
@CommandLine.Command(name = "create",
        header = "%n@|green Create-Controller|@",
        subcommands = {
                CreateController.CreateServiceCommand.class,
                CreateController.CreateSpaceDeveloperCommand.class,
                CreateController.CreateApplicationCommand.class})
public class CreateController implements Runnable {

    @Override
    public void run() {
        // this code is executed if the user runs the create command without specifying any sub-command
    }

    @CommandLine.Command(name = "space-developer",
            description = "Create a space developer in the target space")
    static class CreateSpaceDeveloperCommand implements Runnable {
        @CommandLine.Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            //TODO:Implement functionality
        }
    }

    @CommandLine.Command(name = "service", description = "Create a service in the target space")
    static class CreateServiceCommand implements Runnable {
        @CommandLine.Mixin
        LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public void run() {
            //TODO:Implement functionality
            String yamlFileContent;
            try {
                yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }

            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();
            ServiceBean serviceBean = yamlLoader.loadAs(yamlFileContent, ServiceBean.class);

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            //Create Service
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            try {
                servicesOperations.create(serviceBean);
            } catch (CreationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "application", description = "Create a application in the target space")
    static class CreateApplicationCommand implements Runnable {
        @CommandLine.Mixin
        LoginCommandOptions loginOptions;

        @Override
        public void run() {
            //TODO:Implement functionality
        }
    }

    public static void main(String... args) {
        // CommandLine.run(new GetController.GetServicesCommand(), System.err, args);
        CommandLine.run(new CreateServiceCommand(), System.err, args);

    }

}
