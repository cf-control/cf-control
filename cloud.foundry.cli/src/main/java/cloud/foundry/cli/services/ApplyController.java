package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.ApplyLogic;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the apply commands. They provide the service of manipulating
 * the state of a cloud foundry instance such that it matches with a provided configuration file.
 */
@Command(name = "apply",
        header = "%n@|green Apply the configuration from a given yaml file to your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {
                ApplyController.ApplyServiceCommand.class
                ApplyController.ApplySpaceDevelopersCommand.class,
                ApplyController.ApplyApplicationCommand.class})

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
            SpecBean desiredSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            log.info("YAML file interpreted.");

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplyLogic applyLogic = new ApplyLogic(cfOperations);
            applyLogic.applySpaceDevelopers(desiredSpecBean.getSpaceDevelopers());

            return 0;
        }
    }

    //TODO update the description as soon as the command does more than just creating applications
    @Command(name = "applications", description = "Create applications that are present in the given yaml" +
            " file, but not in your cf instance.")
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
            SpecBean desiredSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            log.info("YAML file interpreted.");

            applyLogic.applyApplications(desiredSpecBean.getApps());

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

            log.info("Interpreting YAML file...");
            SpecBean desiredSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            log.info("YAML file interpreted.");

            applyLogic.applyServices(desiredSpecBean.getServices());

            return 0;
        }
    }

}
