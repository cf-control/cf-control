package cloud.foundry.cli.operations;

import cloud.foundry.cli.operations.applications.ApplicationsOperationsLogging;
import cloud.foundry.cli.operations.applications.DefaultApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultOperationsFactory extends OperationsFactory {

    private DefaultCloudFoundryOperations cfOperations;

    public DefaultOperationsFactory(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
    }

    @Override
    public ApplicationsOperations createApplicationsOperations() {
        DefaultApplicationsOperations defaultApplicationsOperations = new DefaultApplicationsOperations(cfOperations);
        return new ApplicationsOperationsLogging(defaultApplicationsOperations);
    }

    @Override
    public ServicesOperations createServiceOperations() {
        return new ServicesOperations(cfOperations);
    }

    @Override
    public SpaceDevelopersOperations createSpaceDevelopersOperations() {
        return new SpaceDevelopersOperations(cfOperations);
    }

    @Override
    public SpaceOperations createSpaceOperations() {
        return new SpaceOperations(cfOperations);
    }

    @Override
    public ClientOperations createClientOperations() {
        return new ClientOperations(cfOperations);
    }
}
