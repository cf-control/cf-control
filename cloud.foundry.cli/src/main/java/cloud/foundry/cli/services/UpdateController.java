package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.Callable;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import picocli.CommandLine.Option;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.operations.ServicesOperations;
import reactor.core.publisher.Mono;

/**
 * This class realizes the functionality that is needed for the update commands.
 * They provide the service of manipulating the state of a cloud foundry
 * instance such that it matches with a provided configuration file.
 */
@Command(name = "update", header = "%n@|green Update-Controller|@", subcommands = {
    UpdateController.RemoveSpaceDeveloperCommand.class,
    UpdateController.UpdateServiceCommand.class,
    UpdateController.RemoveServiceInstanceCommand.class,
    UpdateController.RemoveApplicationCommand.class,
    UpdateController.UpdateApplicationCommand.class })
public class UpdateController implements Callable<Integer> {

    @Override
    public Integer call() {
        // this code is executed if the user runs the create command without specifying
        // any sub-command
        throw new UnsupportedOperationException("no default operation implemented in UpdateController");
    }

    @Command(name = "remove-space-developer", description = "Removes space developers.")
    static class RemoveSpaceDeveloperCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Removing space developers...");

            SpaceDevelopersBean spaceDevelopersBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(),
                SpaceDevelopersBean.class);

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);

            spaceDevelopersOperations.removeSpaceDeveloper(spaceDevelopersBean.getSpaceDevelopers());
            Log.info("Space Developers removed: ", String.valueOf(spaceDevelopersBean.getSpaceDevelopers()));

            return 0;
        }
    }

    @Command(name = "remove-service", description = "Removes a service instance.")
    static class RemoveServiceInstanceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Option(names = { "-f", "--force" }, required = false, description = "Force deletion without confirmation.")
        Boolean force;

        @Override
        public Integer call() throws Exception {

            if (force != null) {
                doRemoveServiceInstance(yamlCommandOptions);
            } else {
                if (System.console() == null) {
                    Log.error("--force/-f not supplied and not running in terminal, aborting");
                    return 2;
                }

                System.out.println("Really delete the services y/n?");
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                scanner.close();
                if (input.equals("y") || input.equals("yes")) {
                    doRemoveServiceInstance(yamlCommandOptions);
                } else {
                    System.out.println("Delete cancelled");

                    return 1;
                }
            }

            return 0;
        }

        private void doRemoveServiceInstance(YamlCommandOptions yamlCommandOptions) throws Exception {
            Log.info("Removing services...");
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ServiceBean> serviceBeans = specBean.getServices();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            for (Entry<String, ServiceBean> serviceEntry : serviceBeans.entrySet()) {
                String serviceName = serviceEntry.getKey();
                servicesOperations.removeServiceInstance(serviceName);
            }
        }
    }

    @Command(name = "remove-application", description = "Removes an application.")
    static class RemoveApplicationCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Removing applications...");
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ApplicationBean> applicationBeans = specBean.getApps();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
            applicationBeans.keySet().forEach(applicationsOperations::remove);

            return 0;
        }
    }

    @Command(name = "update-service", description = "Updates service instances.")
    static class UpdateServiceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ServiceBean> serviceBeans = specBean.getServices();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            Log.info("Updating services...");
            for (Entry<String, ServiceBean> serviceEntry : serviceBeans.entrySet()) {
                String serviceName = serviceEntry.getKey();
                ServiceBean serviceBean = serviceEntry.getValue();

                // "currentName" is currently a placeholder until diff is implemented
                Mono<Void> toRename = servicesOperations.renameServiceInstance(serviceName, "currentName");
                try {
                    toRename.block();
                } catch (RuntimeException e) {
                    throw new CreationException(e.getMessage());
                }
                Log.info("Service name changed: ", serviceName);
                Mono<Void> toUpdate = servicesOperations.updateServiceInstance(serviceName, serviceBean);
                try {
                    toUpdate.block();
                    Log.info("Service Plan and Tags haven been updated");
                } catch (RuntimeException e) {
                    throw new CreationException(e.getMessage());
                }
                Log.info("Service Plan and Tags haven been updated of service:", serviceName);
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
