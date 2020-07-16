package cloud.foundry.cli.operations;

/**
 * Abstract factory for the operations class module
 */
public abstract class OperationsFactory {

    private static OperationsFactory instance;

    public static OperationsFactory getInstance() {
        return instance;
    }

    public static void setInstance(OperationsFactory instance) {
        OperationsFactory.instance = instance;
    }

    public abstract ApplicationsOperations createApplicationsOperations();

    public abstract ServicesOperations createServiceOperations();

    public abstract SpaceDevelopersOperations createSpaceDevelopersOperations();

    public abstract SpaceOperations createSpaceOperations();

    public abstract ClientOperations createClientOperations();

}
