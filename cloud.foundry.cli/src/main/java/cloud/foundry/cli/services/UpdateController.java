package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.Callable;

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

        private static final Log log = Log.getLog(RemoveSpaceDeveloperCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            log.info("Removing space developers...");

            SpaceDevelopersBean spaceDevelopersBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(),
                SpaceDevelopersBean.class);

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);

            spaceDevelopersOperations.removeSpaceDeveloper(spaceDevelopersBean.getSpaceDevelopers());
            log.info("Space Developers removed: ", String.valueOf(spaceDevelopersBean.getSpaceDevelopers()));

            return 0;
        }
    }

    @Command(name = "remove-service", description = "Removes a service instance.")
    static class RemoveServiceInstanceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(RemoveServiceInstanceCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Option(names = { "-f", "--force" }, required = false, description = "Force deletion without confirmation.")
        Boolean force;

        @Override
        public Integer call() throws Exception {
            Map<String, ServiceBean> services = readServicesFromYamlFile();

            // in case the --force flag is not specified, we shall prompt the user to prevent the removal of services
            // as that might remove valuable data as well
            if (force == null) {
                if (System.console() == null) {
                    log.error("--force/-f not supplied and not running in terminal, aborting");
                    return 2;
                }

                System.out.print("Requested removal of the following services: ");
                for (String serviceName : services.keySet()) {
                    System.out.print("\"" + serviceName + "\" ");
                }
                System.out.println();

                System.out.print("Are you sure you want to permanently delete those? [y|N] ");

                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine();
                scanner.close();

                // turn into lower case, we would accept an upper case response, too
                input = input.toLowerCase();

                if (!(input.equals("y") || input.equals("yes"))) {
                    System.out.println("Cancelled by user");
                    return 1;
                }
            }

            // try to remove all services on a best-effort basis
            // the method should handle errors and log them appropriately
            doRemoveServiceInstance(services);
            
            return 0;
        }

        private Map<String, ServiceBean> readServicesFromYamlFile() throws IOException {
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            return specBean.getServices();
        }

        /**
         * Try to remove service instances. This method handles potential errors internally by logging them.
         * In case it fails, it returns false. The caller can opt to use this to perform additional error handling.
         * @param services services to remove
         * @return true on success, false if there were errors
         */
        private boolean doRemoveServiceInstance(Map<String, ServiceBean> services) {
            log.info("Removing services...");

            DefaultCloudFoundryOperations cfOperations;

            try {
                cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            } catch (Exception e) {
                log.error("Failed to create CF operations: ", e.getMessage());
                return false;
            }

            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            // I'm optimistic at the beginning of this loop
            // prove me wrong!
            boolean success = true;

            for (Entry<String, ServiceBean> serviceEntry : services.entrySet()) {
                String serviceName = serviceEntry.getKey();

                try {
                    servicesOperations.removeServiceInstance(serviceName);
                } catch (Exception e) {
                    log.error("Failed to remove service ", serviceName, ": ", e.getMessage());
                    success = false;
                }
            }

            return success;
        }
    }

    @Command(name = "remove-application", description = "Removes an application.")
    static class RemoveApplicationCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(RemoveApplicationCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            log.info("Removing applications...");
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ApplicationBean> applicationBeans = specBean.getApps();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
            applicationBeans.keySet().forEach(applicationsOperations::removeApplication);

            return 0;
        }
    }

    @Command(name = "update-service", description = "Updates service instances.")
    static class UpdateServiceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(UpdateServiceCommand.class);

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

            log.info("Updating services...");
            for (Entry<String, ServiceBean> serviceEntry : serviceBeans.entrySet()) {
                String serviceName = serviceEntry.getKey();
                ServiceBean serviceBean = serviceEntry.getValue();

                // "currentName" is currently a placeholder until diff is implemented
                servicesOperations.renameServiceInstance(serviceName, "currentName");
                log.info("Service name changed: ", serviceName);
                servicesOperations.updateServiceInstance(serviceName, serviceBean);
                log.info("Service Plan and Tags haven been updated of service:", serviceName);
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
