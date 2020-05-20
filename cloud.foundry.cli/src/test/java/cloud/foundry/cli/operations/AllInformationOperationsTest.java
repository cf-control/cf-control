package cloud.foundry.cli.operations;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
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

import java.util.LinkedList;
import java.util.List;


/**
 * Test for {@link AllInformationOperations}
 */
public class AllInformationOperationsTest {
    @Test
    public void testGetAll() throws Exception {
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
                "  applications:\n" +
                "  - manifest:\n" +
                "      buildpack: null\n" +
                "      command: null\n" +
                "      disk: 0\n" +
                "      dockerImage: null\n" +
                "      dockerUsername: null\n" +
                "      domains: [\n" +
                "        ]\n" +
                "      environmentVariables: {\n" +
                "        }\n" +
                "      healthCheckHttpEndpoint: null\n" +
                "      healthCheckType: null\n" +
                "      hosts: [\n" +
                "        ]\n" +
                "      instances: 0\n" +
                "      memory: 0\n" +
                "      name: null\n" +
                "      noHostname: false\n" +
                "      noRoute: false\n" +
                "      randomRoute: false\n" +
                "      routePath: null\n" +
                "      routes: [\n" +
                "        ]\n" +
                "      services: [\n" +
                "        ]\n" +
                "      stack: null\n" +
                "      timeout: 0\n" +
                "    path: null\n" +
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
        Mono<GetInfoResponse> monoGetInfoResponseMock = mock(Mono.class);
        when(cfClientInfoMock.get(any())).thenReturn(monoGetInfoResponseMock);
        GetInfoResponse getInfoResponseMock = mock(GetInfoResponse.class);
        when(monoGetInfoResponseMock.block()).thenReturn(getInfoResponseMock);
        when(getInfoResponseMock.getApiVersion()).thenReturn("API VERSION");
    }

    private void mockDetermineTarget(DefaultCloudFoundryOperations cfOperationsMock,
                                     ReactorCloudFoundryClient rclMock) {
        // mock for method determineTarget(
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(rclMock);
        DefaultConnectionContext ccMock = mock(DefaultConnectionContext.class);
        when(rclMock.getConnectionContext()).thenReturn(ccMock);
        when(ccMock.getApiHost()).thenReturn("SOME API ENDPOINT");
    }

    private void mockDetermineSpec(DefaultCloudFoundryOperations cfOperationsMock) {
        // space-developers
        SpaceDevelopersOperations spaceDevelopersOperationsMock =
                mock(SpaceDevelopersOperations.class);
        mockSpaceDevelopersOperations(cfOperationsMock);
        SpaceDevelopersBean spaceDevelopersBean = mock(SpaceDevelopersBean.class);
        when(spaceDevelopersOperationsMock.getAll()).thenReturn(spaceDevelopersBean);

        // services
        ServicesOperations servicesOperationsMock = mock(ServicesOperations.class);
        ServiceBean serviceInstanceSummaryBeanMock = mock(ServiceBean.class);
        mockServicesOperations(cfOperationsMock);
        when(servicesOperationsMock.getAll()).thenReturn(singletonList(serviceInstanceSummaryBeanMock));

        // applications
        ApplicationOperations applicationOperationsMock = mock(ApplicationOperations.class);
        ApplicationBean applicationBeanMock = mock(ApplicationBean.class);
        mockApplicationOperations(cfOperationsMock);
        when(applicationOperationsMock.getAll()).thenReturn(singletonList(applicationBeanMock));
    }

    private void mockSpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);
        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);

        Mono<SpaceUsers> monoMock = mock(Mono.class);
        when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);

        SpaceUsers spaceUsersMock = mock(SpaceUsers.class);
        when(monoMock.block()).thenReturn(spaceUsersMock);
    }

    private void mockServicesOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        Services servicesMock = mock(Services.class);
        Flux<ServiceInstanceSummary> flux = mock(Flux.class);
        Mono<List<ServiceInstanceSummary>> mono = mock(Mono.class);
        List<ServiceInstanceSummary> list = new LinkedList<>();

        Mockito.when(cfOperationsMock.services()).thenReturn(servicesMock);
        Mockito.when(servicesMock.listInstances()).thenReturn(flux);
        Mockito.when(flux.collectList()).thenReturn(mono);
        Mockito.when(mono.block()).thenReturn(list);
    }

    private void mockApplicationOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        ApplicationManifest applicationManifestMock = mock(ApplicationManifest.class);
        List<ApplicationManifest> manifest = new LinkedList<>();
        manifest.add(applicationManifestMock);

        ApplicationSummary applicationSummaryMock = mock(ApplicationSummary.class);
        when(applicationSummaryMock.getName()).thenReturn("somename");

        Mono<List<ApplicationSummary>> summaryListMono = mock(Mono.class);
        Mockito.when(summaryListMono.block()).thenReturn(singletonList(applicationSummaryMock));

        Flux<ApplicationSummary> flux = mock(Flux.class);
        Mockito.when(flux.collectList()).thenReturn(summaryListMono);

        Applications applicationsMock = mock(Applications.class);
        Mockito.when(applicationsMock.list()).thenReturn(flux);

        Mockito.when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        Mono<ApplicationManifest> monoMock = mock(Mono.class);

        when(applicationsMock.getApplicationManifest(any())).thenReturn(monoMock);
        when(monoMock.block()).thenReturn(applicationManifestMock);
    }
}
