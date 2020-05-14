package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.Bean;
import org.cloudfoundry.operations.CloudFoundryOperations;

import java.util.List;

public abstract class AbstractOperations<T extends CloudFoundryOperations> {

    protected T cloudFoundryOperations;

    public AbstractOperations(T cloudFoundryOperations) {
        this.cloudFoundryOperations = cloudFoundryOperations;
    }

    public abstract void create(Bean bean);

    public abstract void delete(Bean bean);

    public abstract void update(Bean bean);

    public abstract Bean get(Bean bean);

    public abstract List<? extends Bean> getAll();
}
