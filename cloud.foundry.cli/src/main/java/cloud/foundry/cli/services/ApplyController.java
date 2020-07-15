package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.logic.ApplyLogic;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the apply commands.
 * They provide the service of manipulating the state of a cloud foundry
 * instance such that it matches with a provided configuration file.
 */
@Command(name = "apply",
        header = "%n@|green Apply the configuration from a given yaml file to your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {
                ApplyController.ApplyServiceCommand.class,
                ApplyController.ApplySpaceDevelopersCommand.class,
                ApplyController.ApplyApplicationCommand.class,
                ApplyController.ApplySpaceCommand.class})

public class ApplyController implements Callable<Integer> {

    @Override
    public Integer call() {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "space-developers",
            description = "Assign users as space developers that are present " +
                    "in the given yaml file, but not in your cf instance, or revoke the space developer " +
                    "if its in the cf instance, but not in the yaml file.")
    static class ApplySpaceDevelopersCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(ApplyApplicationCommand.class);

        @Mixin
        private LoginCommandOptions loginOptions;

        @Mixin
        private YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            log.info("Interpreting YAML file...");
            ConfigBean desiredConfigBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(),
                    ConfigBean.class);
            log.verbose("YAML file interpreted.");

            if (desiredConfigBean.getSpec() == null || desiredConfigBean.getSpec().getSpaceDevelopers() == null) {
                log.info("No spaceDevelopers node specified in the yaml file. Nothing to apply...");
                return 0;
            }

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplyLogic applyLogic = new ApplyLogic(cfOperations);
            applyLogic.applySpaceDevelopers(desiredConfigBean.getSpec().getSpaceDevelopers());

            return 0;
        }
    }

    @Command(name = "applications", description = "Apply the differences between the applications given"
        + " in the yaml file and the configuration of the apps of your cf instance")
    static class ApplyApplicationCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(ApplyApplicationCommand.class);

        @Mixin
        private LoginCommandOptions loginOptions;

        @Mixin
        private YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

            ApplyLogic applyLogic = new ApplyLogic(cfOperations);

            log.info("Interpreting YAML file...");
            ConfigBean desiredConfigBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(),
                    ConfigBean.class);
            log.verbose("YAML file interpreted.");

            if (desiredConfigBean.getSpec() == null || desiredConfigBean.getSpec().getApps() == null) {
                log.info("No apps node specified in the yaml file. Nothing to apply...");
                return 0;
            }

            applyLogic.applyApplications(desiredConfigBean.getSpec().getApps());

            return 0;
        }
    }

    @Command(name = "services", description = "Create/remove services that are present in the given yaml" +
            " file, but not in your cf instance.")
    static class ApplyServiceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(ApplyServiceCommand.class);

        @Mixin
        private LoginCommandOptions loginOptions;

        @Mixin
        private YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

            ApplyLogic applyLogic = new ApplyLogic(cfOperations);

            log.verbose("Interpreting YAML file...");
            ConfigBean desiredConfigBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(),
                    ConfigBean.class);
            log.verbose("YAML file interpreted.");

            if (desiredConfigBean.getSpec() == null || desiredConfigBean.getSpec().getServices() == null) {
                log.info("No services node specified in the yaml file. Nothing to apply...");
                return 0;
            }

            applyLogic.applyServices(desiredConfigBean.getSpec().getServices());

            return 0;
        }
    }

    @Command(name = "space", description = "Create a space if it is not present in your cf instance.")
    static class ApplySpaceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(ApplySpaceCommand.class);

        @Mixin
        private LoginCommandOptions loginOptions;

        @Override
        public Integer call() {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplyLogic applyLogic = new ApplyLogic(cfOperations);
            String desiredSpace = loginOptions.getSpace();

            if (desiredSpace != null) {
                applyLogic.applySpace(desiredSpace);
            } else {
                log.info("No space specified.");
            }

            return 0;
        }
    }

}
