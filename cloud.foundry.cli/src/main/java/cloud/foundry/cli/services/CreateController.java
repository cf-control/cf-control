package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

/**
 * This class realizes the functionality that is needed for the create commands.
 */
@CommandLine.Command(name = "create",
        header = "%n@|green Create-Controller|@",
        subcommands = {
                CreateController.CreateServiceCommand.class,
                CreateController.CreateSpaceDeveloperCommand.class,
                CreateController.CreateApplicationCommand.class})
public class CreateController implements Runnable {

    @Override
    public void run() {
        // this code is executed if the user runs the create command without specifying any sub-command
    }

    @CommandLine.Command(name = "space-developer",
            description = "Create a space developer in the target space")
    static class CreateSpaceDeveloperCommand implements Runnable {
        @CommandLine.Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            //TODO:Implement functionality
        }
    }

    @CommandLine.Command(name = "service", description = "Create a service in the target space")
    static class CreateServiceCommand implements Runnable {
        @CommandLine.Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            //TODO:Implement functionality
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(commandOptions);
            //Create Service
            CreateServiceInstanceRequest.Builder createServiceBuilder = CreateServiceInstanceRequest.builder();
            createServiceBuilder.serviceName("elephantsql");
            createServiceBuilder.planName("turtle");
            createServiceBuilder.serviceInstanceName("Elephant");
            Mono<Void> created = cfOperations.services().createInstance(createServiceBuilder.build());
            try {
                created.block();
                System.out.println("Service has been created.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            //Bind apps to service
            BindServiceInstanceRequest.Builder bindServiceBuilder = BindServiceInstanceRequest.builder();
            bindServiceBuilder.applicationName("test-app");
            bindServiceBuilder.serviceInstanceName("Elephant");
            Mono<Void> bind = cfOperations.services().bind(bindServiceBuilder.build());
            try {
                bind.block();
                System.out.println("Service has been binded.");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "application", description = "Create a application in the target space")
    static class CreateApplicationCommand implements Runnable {
        @CommandLine.Mixin
        GetControllerCommandOptions commandOptions;

        @Override
        public void run() {
            //TODO:Implement functionality
        }
    }

    public static void main(String... args) {
        // CommandLine.run(new GetController.GetServicesCommand(), System.err, args);
        CommandLine.run(new CreateServiceCommand(), System.err, args);

    }

}
