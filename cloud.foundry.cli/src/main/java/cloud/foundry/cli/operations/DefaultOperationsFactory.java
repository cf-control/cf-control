package cloud.foundry.cli.operations;

import cloud.foundry.cli.operations.applications.ApplicationsOperationsLogging;
import cloud.foundry.cli.operations.applications.DefaultApplicationsOperations;
import cloud.foundry.cli.operations.services.DefaultServicesOperations;
import cloud.foundry.cli.operations.spacedevelopers.DefaultSpaceDevelopersOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory class for the default operations classes that handle the communication with the cloud foundry instance
 */
public class DefaultOperationsFactory extends OperationsFactory {

    private DefaultCloudFoundryOperations cfOperations;

    /**
     * @param cfOperations the cloud foundry operations instance
     * @throws NullPointerException when the argument is null
     */
    public DefaultOperationsFactory(@Nonnull DefaultCloudFoundryOperations cfOperations) {
        checkNotNull(cfOperations);

        this.cfOperations = cfOperations;
    }

    /**
     * The returned object will have a logging decorator attached.
     *
     * @return instance of the {@ApplicationsOperations} object
     */
    @Override
    public ApplicationsOperations createApplicationsOperations() {
        DefaultApplicationsOperations defaultApplicationsOperations = new DefaultApplicationsOperations(cfOperations);
        return new ApplicationsOperationsLogging(defaultApplicationsOperations);
    }
    /**
     * @return instance of the {@ServicesOperations} object
     */
    @Override
    public ServicesOperations createServiceOperations() {
        return new DefaultServicesOperations(cfOperations);
    }
    /**
     * @return instance of the {@SpaceDevelopersOperations} object
     */
    @Override
    public SpaceDevelopersOperations createSpaceDevelopersOperations() {
        return new SpaceDevelopersOperations(cfOperations);
    }

    /**
     * @return instance of the {@SpaceOperations} object
     */
    @Override
    public SpaceOperations createSpaceOperations() {
        return new SpaceOperations(cfOperations);
    }

    /**
     * @return instance of the {@ClientOperations} object
     */
    @Override
    public ClientOperations createClientOperations() {
        return new ClientOperations(cfOperations);
    }
}
