package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.operations.ServicesOperations;

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

    @Command(name = "remove-space-developer", description = "Removes space developers.")
    static class RemoveSpaceDeveloperCommand implements Callable<Integer> {
        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            SpaceDevelopersBean spaceDevelopersBean = YamlMapper.loadBean(commandOptions.getYamlFilePath(),
                    SpaceDevelopersBean.class);

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);

            spaceDevelopersOperations.removeSpaceDeveloper(spaceDevelopersBean.getSpaceDevelopers());

            return 0;
        }
    }

    @Command(name = "update-service", description = "Updates service instances.")
    static class UpdateServiceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            SpecBean specBean = specBean = YamlMapper.loadBean(commandOptions.getYamlFilePath(), SpecBean.class);
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
        YamlCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            throw new UnsupportedOperationException("update-application has not been implemented yet");
        }
    }
}
