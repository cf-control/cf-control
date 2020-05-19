package cloud.foundry.cli.operations;

import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.junit.jupiter.api.Test;
import org.powermock.api.mockito.PowerMockito;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link AllInformationOperations}
 */
public class AllInformationOperationsTest {

    @Test
    public void bla() throws Exception {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();

        // when
        AllInformationOperations allInformationOperations = new AllInformationOperations(cfOperationsMock);
        String allInformation = YamlCreator.createDefaultYamlProcessor().dump(allInformationOperations.getAll());

        // then
        assertThat(allInformation, is("spaceDevelopers:\n- one\n- two\n- three\n"));
    }

    private DefaultCloudFoundryOperations mockDefaultCloudFoundryOperations() throws Exception {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        when(cfOperationsMock.getSpace()).thenReturn("development");
        when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");

        ReactorCloudFoundryClient rclMock = mock(ReactorCloudFoundryClient.class);
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(rclMock);

        DefaultConnectionContext ccMock = mock(DefaultConnectionContext.class);
        when(rclMock.getConnectionContext()).thenReturn(ccMock);

        when(ccMock.getApiHost()).thenReturn("SOME API MAN");

        Info cfClientInfoMock = mock(Info.class);
        when(rclMock.info()).thenReturn(cfClientInfoMock);

        Mono<GetInfoResponse> monoGetInfoResponseMock = mock(Mono.class);
        when(cfClientInfoMock.get(any())).thenReturn(monoGetInfoResponseMock);

        GetInfoResponse getInfoResponseMock = mock(GetInfoResponse.class);
        when(monoGetInfoResponseMock.block()).thenReturn(getInfoResponseMock);

        when(getInfoResponseMock.getApiVersion()).thenReturn("BLA");


        /*
        ServicesOperations servicesOperationsMock = mock(ServicesOperations.class);
        when(new ServicesOperations(any())).thenReturn(servicesOperationsMock);
        when(servicesOperationsMock.getAll()).then(null);

         */

        SpaceDevelopersOperations mockPoint = mock(SpaceDevelopersOperations.class);


        PowerMockito.whenNew(SpaceDevelopersOperations.class).withAnyArguments().thenReturn(mockPoint);

        SpaceDevelopersBean SpaceDevelopersBean = mock(SpaceDevelopersBean.class);
        when(mockPoint.getAll()).thenReturn(SpaceDevelopersBean);

        return cfOperationsMock;
    }
}
