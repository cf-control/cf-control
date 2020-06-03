package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.operations.ApplicationOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the create commands.
 */
@Command(name = "create", header = "%n@|green Create a new app, service or add a new " +
        "space developer.|@",
        mixinStandardHelpOptions = true,
        subcommands = {
                CreateController.CreateServiceCommand.class,
                CreateController.AssignSpaceDeveloperCommand.class,
                CreateController.CreateApplicationCommand.class})
public class CreateController implements Callable<Integer> {

    @Override
    public Integer call() {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "space-developer", description = "Assign users as space developers.")
    static class AssignSpaceDeveloperCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            String yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());

            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();
            SpaceDevelopersBean spaceDevelopersBean = yamlLoader.loadAs(yamlFileContent, SpaceDevelopersBean.class);

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);

            for (String username : spaceDevelopersBean.getSpaceDevelopers()) {
                spaceDevelopersOperations.assignSpaceDeveloper(username);
            }

            return 0;
        }
    }

    @Command(name = "service", description = "Create a service in the target space.")
    static class CreateServiceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
            String yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());

            Yaml yamlLoader = YamlCreator.createDefaultYamlProcessor();

            Map<String, Object> mapServiceBean = yamlLoader.loadAs(yamlFileContent, Map.class);
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            for (String serviceInstanceName : mapServiceBean.keySet()) {
                String serviceBeanYaml = yamlLoader.dump(mapServiceBean.get(serviceInstanceName));
                ServiceBean serviceBean = yamlLoader.loadAs(serviceBeanYaml, ServiceBean.class);
                servicesOperations.create(serviceInstanceName, serviceBean);
            }

            return 0;
        }
    }

    @Command(name = "application", description = "Create a application in the target space.")
    static class CreateApplicationCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        CreateControllerCommandOptions commandOptions;

        @Override
        public Integer call() throws Exception {
                String yamlFileContent = FileUtils.readLocalFile(commandOptions.getYamlFilePath());

                Yaml yamlProcessor = YamlCreator.createDefaultYamlProcessor();

                Map<String, Object> appMap = yamlProcessor.loadAs(yamlFileContent, Map.class);

                if (appMap.entrySet().iterator().hasNext()) {
                    Map.Entry<String, Object> appObj = appMap.entrySet().iterator().next();
                    String name = appObj.getKey();
                    ApplicationBean applicationBean = yamlProcessor
                            .loadAs(yamlProcessor.dump(appObj.getValue()), ApplicationBean.class);

                    DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
                    ApplicationOperations applicationOperations = new ApplicationOperations(cfOperations);

                    applicationOperations.create(name, applicationBean, false);
                    Log.info("App created:", name);
                    return 0;
                }

                Log.error("App entry in the yaml input file has not a valid format or was missing");
                return 1;
        }
    }

}
