package cloud.foundry.cli.services;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.usage;

import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameResponse;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Interpreting YAML file...");
            SpaceDevelopersBean spaceDevelopersBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(),
                    SpaceDevelopersBean.class);
            Log.info("Loading YAML file...");

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            SpaceDevelopersOperations spaceDevelopersOperations = new SpaceDevelopersOperations(cfOperations);

            List<String> spaceDevelopers = spaceDevelopersBean.getSpaceDevelopers();
            if (spaceDevelopers == null || spaceDevelopers.isEmpty()) {
                Log.info("There are no space developers to assign.");
                return 0;
            }
            if (spaceDevelopers.contains(null)) {
                Log.error("The space developers must not contain null.");
                return 1;
            }

            Log.info("Fetching space id...");
            String spaceId = spaceDevelopersOperations.getSpaceId().block();
            Log.info("Space id fetched.");

            assert (spaceId != null);

            Log.info("Assigning space developers...");

            // signals if any error occurred during the assignment of the space developers
            AtomicReference<Boolean> errorOccurred = new AtomicReference<>(false);

            Flux.fromIterable(spaceDevelopers)
                    .flatMap(spaceDeveloper -> spaceDevelopersOperations.assign(spaceDeveloper, spaceId)
                            .doOnSuccess(response -> onSuccessfulAssignment(spaceDeveloper))
                            .onErrorContinue(this::whenClientException,
                                    (throwable, o) -> onErrorDuringAssignment(throwable, errorOccurred)))
           .blockLast();
            return errorOccurred.get() ? 1 : 0;
        }

        private boolean whenClientException(Throwable throwable) {
            return (throwable instanceof ClientV2Exception);
        }

        private void onSuccessfulAssignment(String spaceDeveloper) {
            Log.verbose(spaceDeveloper, "successfully assigned.");
        }

        private void onErrorDuringAssignment(Throwable exception, AtomicReference<Boolean> errorOccurred) {
            Log.error("An error occurred during space developer assignment:", exception.getMessage());

            // marks that at least a single error has occurred
            errorOccurred.set(true);
        }
    }

    @Command(name = "service", description = "Create services in the target space.")
    static class CreateServiceCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Creating service(s)...");
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);

            Map<String, ServiceBean> serviceBeans = specBean.getServices();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);

            // signals if any error occurred during the assignment of the space developers
            AtomicReference<Boolean> errorOccurred = new AtomicReference<>(false);

            try {
                // do so that authorization has taken place. else leads to authorization problems
                // TODO: find better solution
                cfOperations.getOrganizationId().block();
                Flux.fromIterable(serviceBeans.entrySet())
                        // delay that's used to to avoid race conditions and non-deterministic errors
                        // on the cloud foundry side
                        // TODO: find better solution if possible
                        .delayElements(Duration.ofSeconds(1))
                        .flatMap(serviceEntry ->
                            servicesOperations.create(serviceEntry.getKey(), serviceEntry.getValue()))
                        .onErrorContinue((throwable, o) -> setFlagAndLogError(throwable, errorOccurred))
                        .blockLast();
            } catch (RuntimeException e) {
                throw new CreationException(e.getMessage());
            }

            return errorOccurred.get() ? 1 : 0;
        }

        private void setFlagAndLogError(Throwable throwable, AtomicReference<Boolean> errorOccurred) {
            Log.error(throwable);

            // marks that at least a single error has occurred
            errorOccurred.set(true);
        }
    }

    @Command(name = "application", description = "Create applications in the target space.")
    static class CreateApplicationCommand implements Callable<Integer> {

        @Mixin
        LoginCommandOptions loginOptions;

        @Mixin
        YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            Log.info("Creating application(s)...");
            SpecBean specBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);

            Map<String, ApplicationBean> applicationBeans = specBean.getApps();

            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);

            // signals if any error occurred during the assignment of the space developers
            AtomicReference<Boolean> errorOccurred = new AtomicReference<>(false);

            // do so that authorization has taken place. else leads to authorization problems
            // TODO: find better solution
            cfOperations.getOrganizationId().block();
            Flux.fromIterable(applicationBeans.entrySet())
                    .flatMap(appEntry -> applicationsOperations.create(appEntry.getKey(), appEntry.getValue(), false))
                    .onErrorContinue((throwable, o) -> setFlagAndLogError(throwable, errorOccurred))
                    .blockLast();

            return errorOccurred.get() ? 1 : 0;
        }

        private void setFlagAndLogError(Throwable throwable, AtomicReference<Boolean> errorOccurred) {
            Log.error(throwable);

            // marks that at least a single error has occurred
            errorOccurred.set(true);
        }
    }

}
