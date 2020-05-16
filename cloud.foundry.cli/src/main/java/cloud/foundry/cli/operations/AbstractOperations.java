package cloud.foundry.cli.operations;

import org.cloudfoundry.operations.CloudFoundryOperations;

import java.util.List;

public abstract class AbstractOperations<T extends CloudFoundryOperations> {

    protected T cloudFoundryOperations;

    public AbstractOperations(T cloudFoundryOperations) {
        this.cloudFoundryOperations = cloudFoundryOperations;
    }

    public abstract void create(Bean bean);

    public abstract void delete(Bean bean);

}
