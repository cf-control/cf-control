package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import java.io.IOException;

import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.yaml.snakeyaml.Yaml;

import cloud.foundry.cli.crosscutting.beans.ServiceBean;
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
@Command(name = "update", header = "%n@|green Update-Controller|@", subcommands = {
        UpdateController.RemoveSpaceDeveloperCommand.class,
        UpdateController.UpdateServiceCommand.class,
        UpdateController.UpdateApplicationCommand.class})
public class UpdateController implements Runnable {

    private static final String FAILED_TO_READ_YAML_FILE = "Failed to read YAML file";
    private static final String UNEXPECTED_ERROR_OCCURRED = "Unexpected error occurred";

    @Override
    public void run() {
        // this code is executed if the user runs the create command without specifying
        // any sub-command
    }

    @Command(name = "remove-space-developer", description = "Removes a space developer.")
    static class RemoveSpaceDeveloperCommand implements Runnable {
        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public void run() {
            String yamlFileContent;
            try {
                yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());
            } catch (IOException e) {
                Log.exception(e, FAILED_TO_READ_YAML_FILE);
                return;
            }

            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();
            SpaceDevelopersBean spaceDevelopersBean = yamlLoader.loadAs(yamlFileContent, SpaceDevelopersBean.class);
            try {
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
                SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
                spaceDevelopersOperations.removeSpaceDeveloper(spaceDevelopersBean.getSpaceDevelopers());
            } catch (Exception e) {
                Log.exception(e, UNEXPECTED_ERROR_OCCURRED);
            }
        }
    }

    @Command(name = "update-service", description = "Update a service instance")
    static class UpdateServiceCommand implements Runnable {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public void run() {
            String yamlFileContent;
            try {
                yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());
            } catch (IOException e) {
                Log.exception(e, FAILED_TO_READ_YAML_FILE);
                return;
            }
            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();

            try {
                ServiceBean serviceBean = yamlLoader.loadAs(yamlFileContent, ServiceBean.class);
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

                ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
                servicesOperations.update(serviceBean);
            } catch (Exception e) {
                Log.exception(e, UNEXPECTED_ERROR_OCCURRED);
            }
        }
    }


    @Command(name = "update-application", description = "Update ")
    static class UpdateApplicationCommand implements Runnable {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public void run() {

        }
    }
}
