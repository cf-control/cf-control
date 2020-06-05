package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
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
                servicesOperations.create(serviceName, serviceBean);
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
            SpecBean specBean;
            try {
                specBean = YamlMapper.loadBean(commandOptions.getYamlFilePath(), SpecBean.class);
                //TODO centralize exception handling
            } catch (IOException | RefResolvingException | ConstructorException e) {
                Log.exception(e, "Failed to read YAML file");
                return 1;
            }
            Map<String, ApplicationBean> applicationBeans = specBean.getApps();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            for (Entry<String, ApplicationBean> applicationEntry : applicationBeans.entrySet()) {
                String applicationName = applicationEntry.getKey();
                ApplicationBean applicationBean = applicationEntry.getValue();
                applicationsOperations.create(applicationName, applicationBean, false);
                Log.info("App created:", applicationName);
            }

            return 0;
        }
    }

}
