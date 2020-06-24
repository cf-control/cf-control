package cloud.foundry.cli.logic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import cloud.foundry.cli.operations.SpaceDevelopersOperations;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceType;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

import java.nio.file.Paths;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

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

        GetLogic getLogic = new GetLogic(cfOperationsMock);

        SpaceDevelopersOperations mockSpaceDevelopers = mock(SpaceDevelopersOperations.class);
        when(mockSpaceDevelopers.getAll()).thenReturn(Mono.just(Collections.emptyList()));

        Mono monoServices = Mono.just(new HashMap<Object, ServiceBean>() {
        });
        ServicesOperations mockServices = mock(ServicesOperations.class);
        when(mockServices.getAll()).thenReturn(monoServices);

        Mono monoApplications = Mono.just(new HashMap<Object, ApplicationBean>() {
        });
        ApplicationsOperations mockApplications = mock(ApplicationsOperations.class);
        when(mockApplications.getAll()).thenReturn(monoApplications);

        // when
        ConfigBean configBean = getLogic.getAll(mockSpaceDevelopers, mockServices, mockApplications);

        // then
        assertThat(configBean.getApiVersion(), is("API VERSION"));

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

        SpaceDevelopersOperations mockSpaceDevelopers = mockSpaceDevelopersOperations();
        ServicesOperations mockServices = mockServicesOperations();
        ApplicationsOperations mockApplications = mockApplicationOperations();

        GetLogic getLogic = new GetLogic(cfOperationsMock);

        // when
        ConfigBean configBean = getLogic.getAll(mockSpaceDevelopers, mockServices, mockApplications);

        // then
        assertThat(configBean.getApiVersion(), is("API VERSION"));

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
        assertThat(configBean.getSpec().getApps().get("testApp").getPath(), is(Paths.get("some/path").toString()));
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

    @Test
    public void testGetSpaceDevelopers() {
        // given
        SpaceDevelopersOperations mockSpaceDevelopers = mockSpaceDevelopersOperations();
        GetLogic getLogic = new GetLogic(null);

        // when
        List<String> spaceDevelopers = getLogic.getSpaceDevelopers(mockSpaceDevelopers);

        // then
        assertThat(spaceDevelopers, is(notNullValue()));
        assertThat(spaceDevelopers.size(), is(2));
        assertThat(spaceDevelopers, contains("spaceDeveloper1", "spaceDeveloper2"));
    }

    @Test
    public void testGetServices() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations("API VERSION",
                "SOME API ENDPOINT",
                "development",
                "cloud.foundry.cli");

        ServicesOperations mockServices = mockServicesOperations();
        GetLogic getLogic = new GetLogic(cfOperationsMock);

        // when
        Map<String, ServiceBean> services = getLogic.getServices(mockServices);

        // then
        assertThat(services, is(notNullValue()));
        assertThat(services.size(), is(1));
        assertThat(services.containsKey("appdyn"), is(true));
        assertThat(services.get("appdyn").getService(), is("appdynamics"));
        assertThat(services.get("appdyn").getPlan(), is("apm"));
    }

    @Test
    public void testGetApplications() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations("API VERSION",
                "SOME API ENDPOINT",
                "development",
                "cloud.foundry.cli");

        ApplicationsOperations mockApplications = mockApplicationOperations();
        GetLogic getLogic = new GetLogic(cfOperationsMock);

        // when
        Map<String, ApplicationBean> applications = getLogic.getApplications(mockApplications);

        // then
        assertThat(applications, is(notNullValue()));
        assertThat(applications.size(), is(1));
        assertThat(applications.containsKey("testApp"), is(true));
        assertThat(applications.get("testApp").getPath(), is(Paths.get("some/path").toString()));

        ApplicationManifestBean appManifest = applications.get("testApp").getManifest();
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

    private SpaceDevelopersOperations mockSpaceDevelopersOperations() {
        SpaceDevelopersOperations mockSpaceDevelopers = mock(SpaceDevelopersOperations.class);
        when(mockSpaceDevelopers.getAll()).thenReturn(Mono.just(Arrays.asList("spaceDeveloper1", "spaceDeveloper2")));

        return mockSpaceDevelopers;
    }

    private ServicesOperations mockServicesOperations() {
        ServiceInstance serviceInstanceMock = ServiceInstance.builder()
                .service("appdynamics")
                .id("some-id")
                .type(ServiceInstanceType.MANAGED)
                .plan("apm")
                .name("appdyn")
                .tags(Arrays.asList("tag"))
                .build();

        ServiceBean bean = new ServiceBean(serviceInstanceMock);

        HashMap<String, ServiceBean> map = new HashMap<String, ServiceBean>() {{
            put("appdyn", bean);
        }};

        Mono mono = Mono.just(map);

        ServicesOperations mockServices = mock(ServicesOperations.class);
        when(mockServices.getAll()).thenReturn(mono);

        return mockServices;
    }

    private ApplicationsOperations mockApplicationOperations() {
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

        ApplicationBean bean = new ApplicationBean(applicationManifestMock);

        HashMap<String, ApplicationBean> map = new HashMap<String, ApplicationBean>() {{
            put("testApp", bean);
        }};

        Mono mono = Mono.just(map);

        ApplicationsOperations mockApplications = mock(ApplicationsOperations.class);
        when(mockApplications.getAll()).thenReturn(mono);

        return mockApplications;
    }
}
