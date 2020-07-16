package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.operations.client.DefaultClientOperations;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Test for {@link DefaultClientOperations}
 */
public class DefaultClientOperationsTest {

    @Test
    public void testDetermineApiVersion() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);

        CloudFoundryClient cfClientMock = mock(CloudFoundryClient.class);
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(cfClientMock);

        Info infoMock = mock(Info.class);
        when(cfClientMock.info()).thenReturn(infoMock);

        final String apiVersion = "myApiVersion";
        GetInfoResponse getInfoResponse = GetInfoResponse.builder()
                .apiVersion(apiVersion)
                .build();
        when(infoMock.get(any(GetInfoRequest.class))).thenReturn(Mono.just(getInfoResponse));

        DefaultClientOperations clientOperations = new DefaultClientOperations(cfOperationsMock);

        // when
        Mono<String> apiVersionMono = clientOperations.determineApiVersion();

        // then
        assertThat(apiVersionMono.block(), is(apiVersion));
    }

}
