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

        private static final Log log = Log.getLog(DiffServiceCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            log.info("Diffing service(s)...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            SpecBean loadedSpecBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ServiceBean> desiredServices = loadedSpecBean.getServices();
            SpecBean desiredSpecBean = new SpecBean();
            desiredSpecBean.setServices(desiredServices);
            log.debug("Services Yaml File:", desiredSpecBean);

            GetLogic getLogic = new GetLogic();
            Map<String, ServiceBean> servicesLive = getLogic.getServices(servicesOperations);

            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setServices(servicesLive);
            log.debug("Services current config:", specBeanLive);

            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, desiredSpecBean);
            log.debug("Diff string of services:", output);


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

        private static final Log log = Log.getLog(DiffApplicationCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            log.info("Diffing application(s)...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            SpecBean loadedSpecBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            Map<String, ApplicationBean> desiredApplications = loadedSpecBean.getApps();
            SpecBean desiredSpecBean = new SpecBean();
            desiredSpecBean.setApps(desiredApplications);
            log.debug("Apps Yaml File:", desiredSpecBean);

            GetLogic getLogic = new GetLogic();
            Map<String, ApplicationBean> appsLive = getLogic.getApplications(applicationsOperations);

            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setApps(appsLive);
            log.debug("Apps current config:", specBeanLive);


            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, desiredSpecBean);
            log.debug("Diff string of apps:", output);

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

        private static final Log log = Log.getLog(DiffSpaceDevelopersCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            log.info("Diffing space-developer(s)...");
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevOperations = new SpaceDevelopersOperations(cfOperations);

            SpecBean loadedSpecBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            List<String> desiredSpaceDevelopers = loadedSpecBean.getSpaceDevelopers();
            SpecBean desiredSpecBean = new SpecBean();
            desiredSpecBean.setSpaceDevelopers(desiredSpaceDevelopers);

            log.debug("Space Devs Yaml File:", desiredSpecBean);

            GetLogic getLogic = new GetLogic();
            List<String> spaceDevs = getLogic.getSpaceDevelopers(spaceDevOperations);

            SpecBean specBeanLive = new SpecBean();
            specBeanLive.setSpaceDevelopers(spaceDevs);
            log.debug("Space Devs current config:", specBeanLive);

            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(specBeanLive, desiredSpecBean);
            log.debug("Diff string of space-devs:", output);

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

        private static final Log log = Log.getLog(DiffAllCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ConfigBean desiredConfigBean = YamlMapper.loadBeanFromFile(yamlCommandOptions.getYamlFilePath(),
                    ConfigBean.class);

            log.debug("Desired config:", desiredConfigBean);
            log.info("Fetching all information for target space...");

            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
            ClientOperations clientOperations = new ClientOperations(cfOperations);

            GetLogic getLogic = new GetLogic();
            ConfigBean currentConfigBean = getLogic.getAll(spaceDevelopersOperations, servicesOperations,
                    applicationsOperations, clientOperations, loginOptions);

            log.debug("Current Config:", currentConfigBean);
            log.info("Diffing ...");

            DiffLogic diffLogic = new DiffLogic();
            String output = diffLogic.createDiffOutput(currentConfigBean, desiredConfigBean);
            log.debug("Diff string of config:", output);

            if (output.isEmpty()) {
                System.out.println(NO_DIFFERENCES);
            } else {
                System.out.println(output);
            }

            return 0;
        }
    }

}
