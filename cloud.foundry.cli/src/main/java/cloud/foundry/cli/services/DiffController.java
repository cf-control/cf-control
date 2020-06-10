package cloud.foundry.cli.services;

import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.DiffLogic;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the diff commands. They provide a comparison of the
 * state of a cloud foundry instance with a provided configuration file.
 */
@CommandLine.Command(name = "diff",
        header = "%n@|green Print the differences between the given yaml file" +
                " and the configuration of your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {
                DiffController.DiffApplicationCommand.class})
public class DiffController implements Callable<Integer> {

    private static final String NO_DIFFERENCES = "There are no differences.";

    @Override
    public Integer call() {
        usage(this, System.out);
        return 0;
    }

    @CommandLine.Command(name = "applications", description = "Print the differences between " +
            "the apps given int the yaml file and the configuration of the apps of your cf instance.")
    static class DiffApplicationCommand implements Callable<Integer> {

        @CommandLine.Mixin
        LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            SpecBean specBeanDesired = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ApplicationBean> appsLive = applicationsOperations.getAll().block();

            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setApps(appsLive);

            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, specBeanDesired);

            if (output.isEmpty()) {
                System.out.println(NO_DIFFERENCES);
            } else {
                System.out.println(output);
            }

            return 0;
        }
    }
}
