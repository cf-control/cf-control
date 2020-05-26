package cloud.foundry.cli.services;

import java.io.IOException;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.yaml.snakeyaml.Yaml;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.operations.ServicesOperations;
import picocli.CommandLine;

/**
 * This class realizes the functionality that is needed for the update commands.
 * They provide the service of manipulating the state of a cloud foundry
 * instance such that it matches with a provided configuration file.
 */
@CommandLine.Command(name = "update", header = "%n@|green Update-Controller|@", subcommands = {
                     UpdateController.UpdateServiceCommand.class,
                     UpdateController.UpdateApplicationCommand.class})
public class UpdateController implements Runnable {

    @Override
    public void run() {
        // this code is executed if the user runs the create command without specifying
        // any sub-command
    }

    @CommandLine.Command(name = "update-service", description = "Update a service instance")
    static class UpdateServiceCommand implements Runnable {

        @CommandLine.Mixin
        LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public void run() {
            String yamlFileContent;
            try {
                yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            }
            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();
            
            try {
                ServiceBean serviceBean = yamlLoader.loadAs(yamlFileContent, ServiceBean.class);
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

                ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
                servicesOperations.update(serviceBean);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }


    @CommandLine.Command(name = "update-application", description = "Update ")
    static class UpdateApplicationCommand implements Runnable {

        @CommandLine.Mixin
        LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public void run() {
            
        }
    }
}
