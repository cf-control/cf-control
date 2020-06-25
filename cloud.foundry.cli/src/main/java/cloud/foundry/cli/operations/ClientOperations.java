package cloud.foundry.cli.operations;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Mono;

/**
 * Handles the operations to determine meta-information from a cloud foundry instance.
 * <p>
 * To retrieve the data from resulting Mono or Flux objects you can use subscription methods (block, subscribe, etc.)
 * provided by the reactor library. For more details on how to work with Mono's visit:
 * https://projectreactor.io/docs/core/release/reference/index.html#core-features
 */
public class ClientOperations extends AbstractOperations<DefaultCloudFoundryOperations> {

    public ClientOperations(DefaultCloudFoundryOperations cloudFoundryOperations) {
        super(cloudFoundryOperations);
    }

    /**
     * Determines the API-Version from a cloud foundry instance.
     *
     * @return API-Version
     */
    public Mono<String> determineApiVersion() {
        CloudFoundryClient cfClient = cloudFoundryOperations.getCloudFoundryClient();
        GetInfoRequest infoRequest = GetInfoRequest.builder().build();
        Info cfClientInfo = cfClient.info();
        return cfClientInfo.get(infoRequest).map(GetInfoResponse::getApiVersion);
    }

}
