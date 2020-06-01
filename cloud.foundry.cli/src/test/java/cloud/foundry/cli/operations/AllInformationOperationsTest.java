package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.beans.ConfigBean;
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

import java.util.Collections;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


/**
 * Test for {@link AllInformationOperations}
 */
public class AllInformationOperationsTest {
    @Test
    public void testGetAll() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mockDefaultCloudFoundryOperations();
        AllInformationOperations allInformationOperations = new AllInformationOperations(cfOperationsMock);

        // when
        ConfigBean configBean = allInformationOperations.getAll();

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
