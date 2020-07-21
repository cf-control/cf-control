package cloud.foundry.cli.operations;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;

/**
 * Handles the operations for retrieving target information of a cloud foundry instance.
 */
public class TargetOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public TargetOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * @return the api host of the target cf instance
     */
    public String getApiHost() {
        ReactorCloudFoundryClient reactorClient = (ReactorCloudFoundryClient) this.cloudFoundryOperations
                .getCloudFoundryClient();
        DefaultConnectionContext connectionContext = (DefaultConnectionContext) reactorClient.getConnectionContext();

        return connectionContext.getApiHost();
    }

    /**
     * @return the target organization of the cf instance
     */
    public String getOrganization() {
        return this.cloudFoundryOperations.getOrganization();
    }

    /**
     * @return the target space of the cf instance
     */
    public String getSpace() {
        return this.cloudFoundryOperations.getSpace();
    }

}
