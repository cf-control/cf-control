package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;


/**
 * Test for {@link AllInformationOperations}
 */
public class AllInformationOperationsTest {
    @Test
    public void testGetAll() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();

        // when
        AllInformationOperations allInformationOperations = new AllInformationOperations(cfOperationsMock);
        String allInformation = YamlCreator.createDefaultYamlProcessor().dump(allInformationOperations.getAll());

        // then
        assertThat(allInformation, is("apiVersion: API VERSION\n" +
                "spec:\n" +
                "  spaceDevelopers: [\n" +
                "    ]\n" +
                "  services: [\n" +
                "    ]\n" +
                "  applications: [\n" +
                "    ]\n" +
                "target:\n" +
                "  org: cloud.foundry.cli\n" +
                "  api endpoint: SOME API ENDPOINT\n" +
                "  space: development\n"));
    }

    private DefaultCloudFoundryOperations mockDefaultCloudFoundryOperations() {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        when(cfOperationsMock.getSpace()).thenReturn("development");
        when(cfOperationsMock.getOrganization()).thenReturn("cloud.foundry.cli");
        ReactorCloudFoundryClient rclMock = mock(ReactorCloudFoundryClient.class);

        mockDetermineTarget(cfOperationsMock, rclMock);
        mockDetermineApiVersion(rclMock);
        mockDetermineSpec(cfOperationsMock);

        return cfOperationsMock;
    }

    private void mockDetermineApiVersion(ReactorCloudFoundryClient rclMock) {
        Info cfClientInfoMock = mock(Info.class);

        when(rclMock.info()).thenReturn(cfClientInfoMock);
        when(cfClientInfoMock.get(any()))
                .thenReturn(Mono.just(GetInfoResponse
                    .builder()
                    .apiVersion("API VERSION")
                    .build()));
    }

    private void mockDetermineTarget(DefaultCloudFoundryOperations cfOperationsMock,
                                     ReactorCloudFoundryClient rclMock) {
        DefaultConnectionContext ccMock = mock(DefaultConnectionContext.class);

        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(rclMock);
        when(rclMock.getConnectionContext()).thenReturn(ccMock);
        when(ccMock.getApiHost()).thenReturn("SOME API ENDPOINT");
    }

    private void mockDetermineSpec(DefaultCloudFoundryOperations cfOperationsMock) {
        mockSpaceDevelopersOperations(cfOperationsMock);
        mockServicesOperations(cfOperationsMock);
        mockApplicationOperations(cfOperationsMock);
    }

    private void mockSpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);

        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        when(userAdminMock.listSpaceUsers(any()))
                .thenReturn(Mono.just(SpaceUsers
                        .builder()
                        .developers(Collections.emptyList())
                        .build()));
    }

    private void mockServicesOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        Services servicesMock = mock(Services.class);
        Flux<ServiceInstanceSummary> fluxMock = Flux.fromIterable(Collections.emptyList());

        when(cfOperationsMock.services()).thenReturn(servicesMock);
        when(servicesMock.listInstances()).thenReturn(fluxMock);
    }

    private void mockApplicationOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        Flux<ApplicationSummary> summariesFlux = Flux.fromIterable(Collections.emptyList());
        Applications applicationsMock = mock(Applications.class);

        when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        Mockito.when(applicationsMock.list()).thenReturn(summariesFlux);
    }
}
