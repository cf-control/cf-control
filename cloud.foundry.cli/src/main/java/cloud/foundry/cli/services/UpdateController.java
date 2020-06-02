package cloud.foundry.cli.services;

import java.io.IOException;
import java.util.Map;

import cloud.foundry.cli.crosscutting.logging.Log;
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
@CommandLine.Command(name = "update", header = "%n@|green Update-Controller|@", subcommands = {
    UpdateController.UpdateServiceCommand.class,
    UpdateController.UpdateApplicationCommand.class })
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
                Log.exception(e, "Failed to read YAML file");
                return;
            }
            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();

            try {

                Map<String, Object> mapServiceBean = yamlLoader.loadAs(yamlFileContent, Map.class);
                DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

                ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
                for (String serviceInstanceName : mapServiceBean.keySet()) {

                    String serviceBeanYaml = yamlLoader.dump(mapServiceBean.get(serviceInstanceName));
                    ServiceBean serviceBean = yamlLoader.loadAs(serviceBeanYaml, ServiceBean.class);
                    // "currentName" is currently a placeholder until diff is implemented
                    servicesOperations.renameServiceInstance(serviceInstanceName, "currentName");
                    servicesOperations.updateServiceInstance(serviceInstanceName, serviceBean);

                }
            } catch (Exception e) {
                Log.exception(e, "Unexpected error occurred");
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
