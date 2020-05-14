package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.Bean;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

public abstract class AbstractOperations<T extends CloudFoundryOperations> {

    protected T cloudFoundryOperations;

    public AbstractOperations(T cloudFoundryOperations){
        this.cloudFoundryOperations = cloudFoundryOperations;
    }

    abstract void create(Bean bean);

    abstract void delete(Bean bean);

    abstract void update(Bean bean);

    abstract Object get();
}
