package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.operations.ServicesOperations;
import org.yaml.snakeyaml.constructor.ConstructorException;

/**
 * This class realizes the functionality that is needed for the update commands.
 * They provide the service of manipulating the state of a cloud foundry
 * instance such that it matches with a provided configuration file.
 */
@Command(name = "update", header = "%n@|green Update-Controller|@", subcommands = {
        UpdateController.RemoveSpaceDeveloperCommand.class,
        UpdateController.UpdateServiceCommand.class,
        UpdateController.UpdateApplicationCommand.class})
public class UpdateController implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        // this code is executed if the user runs the create command without specifying
        // any sub-command
        throw new UnsupportedOperationException("no default operation implemented in UpdateController");
    }

    @Command(name = "remove-space-developer", description = "Removes a space developer.")
    static class RemoveSpaceDeveloperCommand implements Callable<Integer> {
        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            SpaceDevelopersBean spaceDevelopersBean;
            try {
                spaceDevelopersBean = YamlMapper.loadBean(commandOptions.getYamlFilePath(), SpaceDevelopersBean.class);
                //TODO centralize exception handling
            } catch (IOException | RefResolvingException | ConstructorException e) {
                Log.exception(e, "Failed to read YAML file");
                return 1;
            }

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);

            spaceDevelopersOperations.removeSpaceDeveloper(spaceDevelopersBean.getSpaceDevelopers());

            return 0;
        }
    }

    @Command(name = "update-service", description = "Update a service instance")
    static class UpdateServiceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            SpecBean specBean;
            try {
                specBean = YamlMapper.loadBean(commandOptions.getYamlFilePath(), SpecBean.class);
                //TODO centralize exception handling
            } catch (IOException | RefResolvingException | ConstructorException e) {
                Log.exception(e, "Failed to read YAML file");
                return 1;
            }
            Map<String, ServiceBean> serviceBeans = specBean.getServices();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            for (Entry<String, ServiceBean> serviceEntry : serviceBeans.entrySet()) {
                String serviceName = serviceEntry.getKey();
                ServiceBean serviceBean = serviceEntry.getValue();

                // "currentName" is currently a placeholder until diff is implemented
                servicesOperations.renameServiceInstance(serviceName, "currentName");
                servicesOperations.updateServiceInstance(serviceName, serviceBean);
            }

            return 0;
        }
    }


    @Command(name = "update-application", description = "Update ")
    static class UpdateApplicationCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            throw new UnsupportedOperationException("update-application has not been implemented yet");
        }
    }
}
