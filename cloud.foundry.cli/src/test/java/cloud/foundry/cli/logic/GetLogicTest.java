package cloud.foundry.cli.logic;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.beans.ConfigBean;
import cloud.foundry.cli.logic.GetLogic;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


/**
 * Test for {@link GetLogic}
 */
public class GetLogicTest {
    @Test
    public void testGetAll() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();
        GetLogic getLogic = new GetLogic(cfOperationsMock);

        // when
        ConfigBean configBean = getLogic.getAll();

        // then
        assertThat(configBean.getApiVersion(), is("API VERSION") );

        assertThat(configBean.getTarget().getEndpoint(), is("SOME API ENDPOINT"));
        assertThat(configBean.getTarget().getOrg(), is("cloud.foundry.cli"));
        assertThat(configBean.getTarget().getSpace(), is("development"));

        assertThat(configBean.getSpec().getSpaceDevelopers().size(), is(2));
        assertThat(configBean.getSpec().getSpaceDevelopers(), contains("xyz@mail.de", "abc@provider.com"));

        assertThat(configBean.getSpec().getServices().size(), is(1));
        assertThat(configBean.getSpec().getServices().containsKey("appdyn"), is(true));
        assertThat(configBean.getSpec().getServices().get("appdyn").getService(), is("appdynamics"));
        assertThat(configBean.getSpec().getServices().get("appdyn").getPlan(), is("apm"));
        assertThat(configBean.getSpec().getServices().get("appdyn").getTags().size(), is(2));
        assertThat(configBean.getSpec().getServices().get("appdyn").getTags(), contains("tag1", "tag2"));

        assertThat(configBean.getSpec().getApps().size(), is(1));
        assertThat(configBean.getSpec().getApps().containsKey("testApp"), is(true));
        assertThat(configBean.getSpec().getApps().get("testApp").getPath(), is("some/path"));
        ApplicationManifestBean appManifest = configBean.getSpec().getApps().get("testApp").getManifest();
        assertThat(appManifest.getBuildpack(), is("buildpack"));
        assertThat(appManifest.getDisk(), is(1024));
        assertThat(appManifest.getEnvironmentVariables().size(), is(1));
        assertThat(appManifest.getEnvironmentVariables().get("key"), is("value"));
        assertThat(appManifest.getHealthCheckType(), is(ApplicationHealthCheck.HTTP));
        assertThat(appManifest.getInstances(), is(3));
        assertThat(appManifest.getMemory(), is(1024));
        assertThat(appManifest.getRandomRoute(), is(true));
        assertThat(appManifest.getServices().size(), is(1));
        assertThat(appManifest.getServices().get(0), is("appdynamics"));
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
        mockSpaceDevelopersOperations(cfOperationsMock);
        mockServicesOperations(cfOperationsMock);
        mockApplicationOperations(cfOperationsMock);
    }

    private void mockSpaceDevelopersOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);
        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);

        Mono<SpaceUsers> monoMock = mock(Mono.class);
        when(userAdminMock.listSpaceUsers(any())).thenReturn(monoMock);

        SpaceUsers spaceUsers = SpaceUsers
                .builder()
                .addAllDevelopers(List.of("xyz@mail.de", "abc@provider.com"))
                .build();
        when(monoMock.block()).thenReturn(spaceUsers);
    }

    private void mockServicesOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        Services servicesMock = mock(Services.class);
        Flux<ServiceInstanceSummary> flux = mock(Flux.class);
        Mono<List<ServiceInstanceSummary>> monoServiceInstanceSummary = mock(Mono.class);
        ServiceInstanceSummary serviceInstanceSummary = ServiceInstanceSummary.builder()
                .service("appdynamics")
                .id("some-id")
                .type(ServiceInstanceType.MANAGED)
                .plan("apm")
                .name("appdyn")
                .build();

        ServiceInstance serviceInstance = ServiceInstance.builder()
                    .service("appdynamics")
                    .id("some-id")
                    .name("appdyn")
                    .type(ServiceInstanceType.MANAGED)
                    .plan("apm")
                    .tags("tag1", "tag2")
                    .build();

        when(cfOperationsMock.services()).thenReturn(servicesMock);
        when(servicesMock.listInstances()).thenReturn(flux);
        when(flux.collectList()).thenReturn(monoServiceInstanceSummary);
        when(monoServiceInstanceSummary.block()).thenReturn(List.of(serviceInstanceSummary));

        Mono monoServiceInstance = mock(Mono.class);
        Mockito.when(servicesMock.getInstance(any())).thenReturn(monoServiceInstance);
        Mockito.when(monoServiceInstance.block()).thenReturn(serviceInstance);
    }

    private void mockApplicationOperations(DefaultCloudFoundryOperations cfOperationsMock) {
        ApplicationManifest applicationManifestMock = ApplicationManifest.builder()
                .name("testApp")
                .buildpack("buildpack")
                .disk(1024)
                .environmentVariable("key", "value")
                .healthCheckType(ApplicationHealthCheck.HTTP)
                .instances(3)
                .memory(1024)
                .path(Paths.get("some/path"))
                .randomRoute(true)
                .services("appdynamics")
                .build();
        List<ApplicationManifest> manifest = new LinkedList<>();
        manifest.add(applicationManifestMock);

        ApplicationSummary applicationSummaryMock = mock(ApplicationSummary.class);
        when(applicationSummaryMock.getName()).thenReturn("testApp");

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
