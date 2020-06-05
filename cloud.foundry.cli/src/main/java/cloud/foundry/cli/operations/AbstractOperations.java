package cloud.foundry.cli.operations;

import org.cloudfoundry.operations.CloudFoundryOperations;

/**
 * The base class of all operation classes. An operation class provides an interface for manipulating a certain aspect
 * of a cloud foundry instance. {@link cloud.foundry.cli.crosscutting.mapping.beans.Bean Beans} should be used to
 * provide and retrieve data.
 * @param <T> The concrete operations class that is used in the derived class to communicate to the cloud foundry
 *           instance
 */
public abstract class AbstractOperations<T  extends CloudFoundryOperations> {

    protected T cloudFoundryOperations;

    public AbstractOperations(T cloudFoundryOperations) {
        this.cloudFoundryOperations = cloudFoundryOperations;
    }

    //TODO: find common methods at a later time and create a common interface for all operation classes

}
