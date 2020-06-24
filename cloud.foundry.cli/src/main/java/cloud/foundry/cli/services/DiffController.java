package cloud.foundry.cli.services;

import static picocli.CommandLine.usage;
import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.DiffLogic;
import cloud.foundry.cli.logic.GetLogic;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ClientOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the diff commands. They provide a comparison of the
 * state of a cloud foundry instance with a provided configuration file.
 */
@Command(name = "diff",
        header = "%n@|green Print the differences between the given yaml file" +
                " and the configuration of your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {
                DiffController.DiffApplicationCommand.class,
                DiffController.DiffSpaceDevelopersCommand.class,
                DiffController.DiffServiceCommand.class,
                DiffController.DiffAllCommand.class})
public class DiffController implements Callable<Integer> {

    private static final String NO_DIFFERENCES = "There are no differences.";

    @Override
    public Integer call() {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "services", description = "Print the differences between " +
            "the services given in the yaml file and the configuration of the services of your cf instance.")
    static class DiffServiceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Diffing service(s)...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            SpecBean loadedSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ServiceBean> desiredServices = loadedSpecBean.getServices();
            SpecBean desiredSpecBean = new SpecBean();
            desiredSpecBean.setServices(desiredServices);
            Log.debug("Services Yaml File:", desiredSpecBean);

            Map<String, ServiceBean> servicesLive = servicesOperations.getAll().block();

            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setServices(servicesLive);
            Log.debug("Services current config:", specBeanLive);

            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, desiredSpecBean);
            Log.debug("Diff string of services:", output);


            if (output.isEmpty()) {
                System.out.println(NO_DIFFERENCES);
            } else {
                System.out.println(output);
            }

            return 0;
        }
    }

    @Command(name = "applications", description = "Print the differences between " +
            "the apps given in the yaml file and the configuration of the apps of your cf instance.")
    static class DiffApplicationCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Diffing application(s)...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            SpecBean loadedSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ApplicationBean> desiredApplications = loadedSpecBean.getApps();
            SpecBean desiredSpecBean = new SpecBean();
            desiredSpecBean.setApps(desiredApplications);
            Log.debug("Apps Yaml File:", desiredSpecBean);

            Map<String, ApplicationBean> appsLive = applicationsOperations.getAll().block();

            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setApps(appsLive);
            Log.debug("Apps current config:", specBeanLive);


            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, desiredSpecBean);
            Log.debug("Diff string of apps:", output);

            if (output.isEmpty()) {
                System.out.println(NO_DIFFERENCES);
            } else {
                System.out.println(output);
            }

            return 0;
        }
    }

    @Command(name = "space-developers", description = "Print the differences between " +
            "the space developers given in the yaml file and the space developers of your cf instance.")
    static class DiffSpaceDevelopersCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Diffing space-developer(s)...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevOperations = new SpaceDevelopersOperations(cfOperations);

            SpecBean loadedSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            List<String> desiredSpaceDevelopers = loadedSpecBean.getSpaceDevelopers();
            SpecBean desiredSpecBean = new SpecBean();
            desiredSpecBean.setSpaceDevelopers(desiredSpaceDevelopers);

            Log.debug("Space Devs Yaml File:", desiredSpecBean);

            List<String> spaceDevs = spaceDevOperations.getAll().block();
            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setSpaceDevelopers(spaceDevs);
            Log.debug("Space Devs current config:", specBeanLive);

            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, desiredSpecBean);
            Log.debug("Diff string of space-devs:", output);

            if (output.isEmpty()) {
                System.out.println(NO_DIFFERENCES);
            } else {
                System.out.println(output);
            }

            return 0;
        }
    }


    @Command(name = "all", description = "Print the differences between " +
            "the config given in the yaml file and the current config of your cf instance.")
    static class DiffAllCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ConfigBean desiredConfigBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), ConfigBean.class);
            Log.debug("Desired config:", desiredConfigBean);

            Log.info("Fetching all information for target space...");

            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
            ClientOperations clientOperations = new ClientOperations(cfOperations);

            GetLogic getLogic = new GetLogic();
            ConfigBean currentConfigBean = getLogic.getAll(spaceDevelopersOperations, servicesOperations,
                    applicationsOperations, clientOperations, loginOptions);
            Log.debug("Current Config:", currentConfigBean);

            Log.info("Diffing ...");
            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(currentConfigBean, desiredConfigBean);
            Log.debug("Diff string of config:", output);

            if (output.isEmpty()) {
                System.out.println(NO_DIFFERENCES);
            } else {
                System.out.println(output);
            }

            return 0;
        }
    }

}
