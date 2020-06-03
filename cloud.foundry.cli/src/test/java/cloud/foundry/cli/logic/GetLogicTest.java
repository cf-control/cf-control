package cloud.foundry.cli.logic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;


/**
 * Test for {@link GetLogic}
 */
public class GetLogicTest {

    @Test
    public void testGetAllWithoutConfigurationData() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations("API VERSION",
                "SOME API ENDPOINT",
                "development",
                "cloud.foundry.cli");


        mockServicesOperations(Collections.emptyList(), cfOperationsMock);
        mockApplicationOperations(Collections.emptyList(), cfOperationsMock);
        mockSpaceDevelopersOperations(Collections.emptyList(), cfOperationsMock);

        GetLogic getLogic = new GetLogic(cfOperationsMock);

        // when
        ConfigBean configBean = getLogic.getAll();

        // then
        assertThat(configBean.getApiVersion(), is("API VERSION") );

        assertThat(configBean.getTarget().getEndpoint(), is("SOME API ENDPOINT"));
        assertThat(configBean.getTarget().getOrg(), is("cloud.foundry.cli"));
        assertThat(configBean.getTarget().getSpace(), is("development"));

        assertTrue(configBean.getSpec().getApps().isEmpty());
        assertTrue(configBean.getSpec().getServices().isEmpty());
        assertTrue(configBean.getSpec().getSpaceDevelopers().isEmpty());
    }

    @Test
    public void testGetAllWithConfigurationData() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations("API VERSION",
                "SOME API ENDPOINT",
                "development",
                "cloud.foundry.cli");

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

        ServiceInstanceSummary serviceInstanceSummary = ServiceInstanceSummary.builder()
                .service("appdynamics")
                .id("some-id")
                .type(ServiceInstanceType.MANAGED)
                .plan("apm")
                .name("appdyn")
                .build();

        mockServicesOperations(Arrays.asList(serviceInstanceSummary), cfOperationsMock);
        mockApplicationOperations(Arrays.asList(applicationManifestMock), cfOperationsMock);
        mockSpaceDevelopersOperations(Arrays.asList("spaceDeveloper1", "spaceDeveloper2"), cfOperationsMock);

        GetLogic getLogic = new GetLogic(cfOperationsMock);

        // when
        ConfigBean configBean = getLogic.getAll();

        // then
        assertThat(configBean.getApiVersion(), is("API VERSION") );

        assertThat(configBean.getTarget().getEndpoint(), is("SOME API ENDPOINT"));
        assertThat(configBean.getTarget().getOrg(), is("cloud.foundry.cli"));
        assertThat(configBean.getTarget().getSpace(), is("development"));

        assertThat(configBean.getSpec().getSpaceDevelopers().size(), is(2));
        assertThat(configBean.getSpec().getSpaceDevelopers(), contains("spaceDeveloper1", "spaceDeveloper2"));

        assertThat(configBean.getSpec().getServices().size(), is(1));
        assertThat(configBean.getSpec().getServices().containsKey("appdyn"), is(true));
        assertThat(configBean.getSpec().getServices().get("appdyn").getService(), is("appdynamics"));
        assertThat(configBean.getSpec().getServices().get("appdyn").getPlan(), is("apm"));

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

    private DefaultCloudFoundryOperations mockDefaultCloudFoundryOperations(String apiVersion,
                                                                            String apiHost,
                                                                            String space,
                                                                            String org) {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        when(cfOperationsMock.getSpace()).thenReturn(space);
        when(cfOperationsMock.getOrganization()).thenReturn(org);
        ReactorCloudFoundryClient rclMock = mock(ReactorCloudFoundryClient.class);

        mockDetermineTarget(apiHost, cfOperationsMock, rclMock);
        mockDetermineApiVersion(apiVersion, rclMock);

        return cfOperationsMock;
    }

    private void mockDetermineApiVersion(String apiVersion, ReactorCloudFoundryClient rclMock) {
        Info cfClientInfoMock = mock(Info.class);

        when(rclMock.info()).thenReturn(cfClientInfoMock);
        when(cfClientInfoMock.get(any()))
                .thenReturn(Mono.just(GetInfoResponse
                    .builder()
                    .apiVersion(apiVersion)
                    .build()));
    }

    private void mockDetermineTarget(String apiHost, DefaultCloudFoundryOperations cfOperationsMock,
                                     ReactorCloudFoundryClient rclMock) {
        DefaultConnectionContext ccMock = mock(DefaultConnectionContext.class);

        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(rclMock);
        when(rclMock.getConnectionContext()).thenReturn(ccMock);
        when(ccMock.getApiHost()).thenReturn(apiHost);
    }

    private void mockSpaceDevelopersOperations(List<String> spaceDevelopers,
                                               DefaultCloudFoundryOperations cfOperationsMock) {
        UserAdmin userAdminMock = mock(UserAdmin.class);
        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);

        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);
        when(userAdminMock.listSpaceUsers(any()))
                .thenReturn(Mono.just(SpaceUsers
                        .builder()
                        .developers(spaceDevelopers)
                        .build()));
    }

    private void mockServicesOperations(List<ServiceInstanceSummary> services,
                                        DefaultCloudFoundryOperations cfOperationsMock) {
        Services servicesMock = mock(Services.class);
        Flux<ServiceInstanceSummary> flux = Flux.fromIterable(services);

        when(cfOperationsMock.services()).thenReturn(servicesMock);
        when(servicesMock.listInstances()).thenReturn(flux);
    }

    private void mockApplicationOperations(List<ApplicationManifest> applicationManifests,
                                           DefaultCloudFoundryOperations cfOperationsMock) {
        Applications applicationsMock = mock(Applications.class);

        Flux<ApplicationManifest> applicationManifestFlux = Flux.fromIterable(applicationManifests);
        Flux applicationSummaryFlux = mock(Flux.class);

        when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        when(applicationsMock.list()).thenReturn(applicationSummaryFlux);
        when(applicationSummaryFlux.flatMap(any())).thenReturn(applicationManifestFlux);
    }
}
