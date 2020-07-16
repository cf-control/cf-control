package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cloud.foundry.cli.mocking.ApplicationsMockBuilder;
import cloud.foundry.cli.mocking.ApplicationsV3MockBuilder;
import cloud.foundry.cli.mocking.CloudFoundryClientMockBuilder;
import cloud.foundry.cli.mocking.DefaultCloudFoundryOperationsMockBuilder;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.operations.applications.DefaultApplicationsOperations;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.applications.CreateApplicationRequest;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.domains.Domains;
import org.cloudfoundry.operations.domains.Status;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.Routes;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link DefaultApplicationsOperations}
 */
public class DefaultApplicationsOperationsTest {

    private static final String SOME_APPLICATION = "SOME_APPLICATION";

    @Test
    public void testGetApplicationsWithEmptyMockData() {
        // prepare mock CF API client with an empty applications list
        Applications applicationsMock = ApplicationsMockBuilder.get().setApps(Collections.emptyMap()).build();
        DefaultCloudFoundryOperations cfMock = DefaultCloudFoundryOperationsMockBuilder
                .get()
                .setApplications(applicationsMock)
                .build();

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfMock);
        Map<String, ApplicationBean> apps = applicationsOperations.getAll().block();

        // check if it's really empty
        assertTrue(apps.isEmpty());
    }

    @Test
    public void testGetApplicationsWithMockData() {
        // given
        // create a mock CF API client
        // first, we need to prepare some ApplicationSummary, Metadata and ApplicationManifest
        // (we're fine with one of both for now)
        // those are then used to create a CF mock API object, which will be able to
        // return those then the right way
        ApplicationManifest appManifest = createMockApplicationManifest();
        Metadata metadata = Metadata
                .builder()
                .annotation(ApplicationBean.PATH_KEY, "/test/uri")
                .annotation(ApplicationBean.METADATA_KEY, "notyetrandomname,1.0.1,some/branch")
                .build();
        DefaultCloudFoundryOperations cfMock = getCloudFoundryOperationsMock(
                Collections.singletonMap("notyetrandomname", appManifest),
                Collections.singletonMap("notyetrandomname", metadata),
                null
        );

        // now, we can generate a YAML doc for our ApplicationSummary
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfMock);

        // when
        Map<String, ApplicationBean> apps = applicationsOperations.getAll().block();

        // then
        // ... and make sure it contains exactly what we'd expect
        assertThat(apps.size(), is(1));
        assertTrue(apps.containsKey("notyetrandomname"));
        ApplicationBean appBean = apps.get("notyetrandomname");
        assertThat(appBean.getPath(), is("/test/uri"));
        assertThat(appBean.getManifest().getBuildpack(), is("test_buildpack"));
        assertThat(appBean.getManifest().getCommand(), is("test command"));
        assertThat(appBean.getManifest().getDisk(), is(1234));
        assertThat(appBean.getManifest().getEnvironmentVariables().size(), is(1));
        assertThat(appBean.getManifest().getEnvironmentVariables().get("key"), is("value"));
        assertThat(appBean.getManifest().getHealthCheckHttpEndpoint(), is("http://healthcheck.local"));
        assertThat(appBean.getManifest().getHealthCheckType(), is(ApplicationHealthCheck.HTTP));
        assertThat(appBean.getManifest().getInstances(), is(42));
        assertThat(appBean.getManifest().getMemory(), is(Integer.MAX_VALUE));
        assertThat(appBean.getManifest().getNoRoute(), is(false));
        assertThat(appBean.getManifest().getRandomRoute(), is(true));
        assertThat(appBean.getManifest().getRoutes(), contains("route1", "route2"));
        assertThat(appBean.getManifest().getServices(), contains("servicealpha", "serviceomega"));
        assertThat(appBean.getManifest().getStack(), is("nope"));
        assertThat(appBean.getManifest().getTimeout(), is(987654321));
        assertThat(appBean.getMeta(), is("notyetrandomname,1.0.1,some/branch"));
        verify(cfMock.applications(), times(1)).list();
    }

    @Test
    public void testCreateSucceeds() throws CreationException {
        // given
        ApplicationManifest appManifest = createMockApplicationManifest();
        Metadata metadata = Metadata
                .builder()
                .annotation(ApplicationBean.PATH_KEY, "some/path")
                .annotation(ApplicationBean.METADATA_KEY, "somemeta")
                .build();

        Applications applicationsMock = ApplicationsMockBuilder.get().build();
        ApplicationsV3 applicationsV3Mock = ApplicationsV3MockBuilder.get().build();
        CloudFoundryClient cfcMock = CloudFoundryClientMockBuilder.get()
                .setApplicationsV3(applicationsV3Mock)
                .build();
        DefaultCloudFoundryOperations dcfoMock = DefaultCloudFoundryOperationsMockBuilder.get()
                .setApplications(applicationsMock)
                .setSpaceId("spaceId")
                .setCloudFoundryClient(cfcMock)
                .build();

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(dcfoMock);

        ApplicationBean applicationsBean = new ApplicationBean(appManifest, metadata);
        applicationsBean.setPath("some/path");
        applicationsBean.setMeta("somemeta");

        //when
        Mono<Void> request = applicationsOperations.create(appManifest.getName(), applicationsBean, false);
        request.block();

        // then
        verify(dcfoMock.applications(), times(1))
                .pushManifest(any(PushApplicationManifestRequest.class));
        verify(applicationsV3Mock, times(1))
                .create(any(CreateApplicationRequest.class));
        verify(dcfoMock, times(1))
                .getSpaceId();
    }

    @Test
    public void testCreateThrowsCreationExceptionWhenNonRecoverableErrorOccurs() {
        //given
        ApplicationManifest appManifest = createMockApplicationManifest();
        Metadata metadata = Metadata
                .builder()
                .annotation(ApplicationBean.PATH_KEY, "some/path")
                .annotation(ApplicationBean.METADATA_KEY, "somemeta")
                .build();

        DefaultCloudFoundryOperations dcfoMock = DefaultCloudFoundryOperationsMockBuilder.get()
                .build();

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(dcfoMock);

        ApplicationBean applicationsBean = new ApplicationBean(appManifest, metadata);
        // create error condition
        applicationsBean.setPath(null);
        applicationsBean.getManifest().setBuildpack(null);

        //when
        assertThrows(CreationException.class,
                () -> applicationsOperations.create(appManifest.getName(), applicationsBean, false));
    }

    @Test
    public void testCreateOnNullNameThrowsNullPointerException() throws CreationException {
        // given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                DefaultCloudFoundryOperationsMockBuilder.get().build());

        //then
        assertThrows(NullPointerException.class,
                () -> applicationsOperations.create(null, new ApplicationBean(), false));
    }

    @Test
    public void testCreateOnEmptyNameThrowsIllegalArgumentException() {
        // given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                DefaultCloudFoundryOperationsMockBuilder.get().build());

        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setPath("some/path");

        // then
        assertThrows(IllegalArgumentException.class,
            () -> applicationsOperations.create("", applicationBean, false));
    }

    @Test
    public void testCreateOnNullBeanThrowsNullPointerException() {
        // given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                DefaultCloudFoundryOperationsMockBuilder.get().build());

        // when
        assertThrows(NullPointerException.class, () -> applicationsOperations.create("appName", null, false));
    }

    @Test
    public void testRemoveApplication() {
        // given
        Applications applicationsMock = ApplicationsMockBuilder.get().build();
        DefaultCloudFoundryOperations cfoMock = DefaultCloudFoundryOperationsMockBuilder
                .get()
                .setApplications(applicationsMock)
                .build();
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfoMock);

        // when
        Mono<Void> request = applicationsOperations.remove(SOME_APPLICATION);

        // then
        assertThat(request, notNullValue());
        verify(cfoMock.applications(), times(1)).delete(any(DeleteApplicationRequest.class));
    }

    @Test
    public void testRemoveApplicationShouldThrowNullPointerExceptionWhenApplicationNameIsNull() {
        // given
        DefaultCloudFoundryOperations cfoMock = DefaultCloudFoundryOperationsMockBuilder.get().build();
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfoMock);

        // when -> then
        assertThrows(NullPointerException.class, () -> applicationsOperations.remove(null));
    }

    @Test
    public void testRenameWithNullValueForCurrentNameThrowsNullPointerExceptionn() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.rename("appName", null));
    }

    @Test
    public void testRenameWithNullValueForNewNameThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.rename("appName", null));
    }

    @Test
    public void testRenameSucceeds() {
        // given
        DefaultCloudFoundryOperations cfoMock = getCloudFoundryOperationsMock(Collections.emptyMap(),
                Collections.emptyMap(),
                null);

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfoMock);

        // when
        Mono<Void> requestResult = applicationsOperations.rename("newName", SOME_APPLICATION);

        // then
        assertThat(requestResult, is(notNullValue()));

        RenameApplicationRequest renameRequest =  RenameApplicationRequest.builder()
                .name(SOME_APPLICATION)
                .newName("newName")
                .build();
        verify(cfoMock.applications(), times(1)).rename(renameRequest);
    }

    @Test
    public void testScaleSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = getCloudFoundryOperationsMock(Collections.emptyMap(),
                Collections.emptyMap(),
                null);

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        final Integer diskLimit = 42;
        final Integer memoryLimit = 112;
        final Integer instances = 3;

        // when
        Mono<Void> scaleResult = applicationsOperations.scale(SOME_APPLICATION, diskLimit, memoryLimit, instances);

        // then
        assertThat(scaleResult, is(notNullValue()));

        ScaleApplicationRequest scaleApplicationRequest = ScaleApplicationRequest
                .builder()
                .name(SOME_APPLICATION)
                .instances(instances)
                .diskLimit(diskLimit)
                .memoryLimit(memoryLimit)
                .build();
        verify(cfOperationsMock.applications(), times(1)).scale(scaleApplicationRequest);
    }

    @Test
    public void testScaleSucceedsWithNullArguments() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = getCloudFoundryOperationsMock(Collections.emptyMap(),
                Collections.emptyMap(),
                null);

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> scaleResult = applicationsOperations.scale(SOME_APPLICATION, null, null, null);

        // then
        assertThat(scaleResult, is(notNullValue()));

        ScaleApplicationRequest scaleRequest = ScaleApplicationRequest
                .builder()
                .name(SOME_APPLICATION)
                .build();
        verify(cfOperationsMock.applications(), times(1)).scale(scaleRequest);
    }

    @Test
    public void testScaleWithNullValueAsApplicationNameThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.scale(null, 12, 34, 56));
    }

    @Test
    public void testAddEnvironmentVariableSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = mock(Applications.class);
        when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        when(applicationsMock.setEnvironmentVariable(any(SetEnvironmentVariableApplicationRequest.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> addEnvVarResult = applicationsOperations
                .addEnvironmentVariable(SOME_APPLICATION, "newVar", "newVal");

        // then
        assertThat(addEnvVarResult, is(notNullValue()));

        SetEnvironmentVariableApplicationRequest request = SetEnvironmentVariableApplicationRequest
                .builder()
                .name(SOME_APPLICATION)
                .variableName("newVar")
                .variableValue("newVal")
                .build();
        verify(applicationsMock, times(1)).setEnvironmentVariable(request);
    }

    @Test
    public void testAddEnvironmentVariableWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.addEnvironmentVariable(null, "var", "val"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.addEnvironmentVariable("app", null, "val"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.addEnvironmentVariable("app", "var", null));
    }

    @Test
    public void testRemoveEnvironmentVariableSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = mock(Applications.class);
        when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        when(applicationsMock.unsetEnvironmentVariable(any(UnsetEnvironmentVariableApplicationRequest.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> removeEnvVarResult = applicationsOperations
                .removeEnvironmentVariable(SOME_APPLICATION, "varToRemove");

        // then
        assertThat(removeEnvVarResult, is(notNullValue()));

        UnsetEnvironmentVariableApplicationRequest request = UnsetEnvironmentVariableApplicationRequest
                .builder()
                .name(SOME_APPLICATION)
                .variableName("varToRemove")
                .build();
        verify(applicationsMock, times(1)).unsetEnvironmentVariable(request);
    }

    @Test
    public void testRemoveEnvironmentVariableWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.removeEnvironmentVariable(null, "varToRemove"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.removeEnvironmentVariable("app", null));
    }

    @Test
    public void testSetHealthCheckSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = mock(Applications.class);
        when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        when(applicationsMock.setHealthCheck(any(SetApplicationHealthCheckRequest.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        ApplicationHealthCheck desiredHealthCheckType = mock(ApplicationHealthCheck.class);

        // when
        Mono<Void> setHealthCheckResult = applicationsOperations
                .setHealthCheck(SOME_APPLICATION, desiredHealthCheckType);

        // then
        assertThat(setHealthCheckResult, is(notNullValue()));

        SetApplicationHealthCheckRequest healthCheckRequest = SetApplicationHealthCheckRequest
                .builder()
                .name(SOME_APPLICATION)
                .type(desiredHealthCheckType)
                .build();
        verify(applicationsMock, times(1)).setHealthCheck(healthCheckRequest);
    }

    @Test
    public void testSetHealthCheckWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.setHealthCheck(null, mock(ApplicationHealthCheck.class)));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.setHealthCheck("app", null));
    }

    @Test
    public void testBindAppSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        when(cfOperationsMock.services()).thenReturn(servicesMock);
        when(servicesMock.bind(any(BindServiceInstanceRequest.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> bindToServiceResult = applicationsOperations.bindToService("someApplication", "someService");

        // then
        assertThat(bindToServiceResult, is(notNullValue()));

        BindServiceInstanceRequest request = BindServiceInstanceRequest
                .builder()
                .applicationName("someApplication")
                .serviceInstanceName("someService")
                .build();
        verify(servicesMock, times(1)).bind(request);
    }

    @Test
    public void testBindAppWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.bindToService(null, "someService"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.bindToService("someApp", null));
    }

    @Test
    public void testUnbindAppSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Services servicesMock = mock(Services.class);
        when(cfOperationsMock.services()).thenReturn(servicesMock);
        when(servicesMock.unbind(any(UnbindServiceInstanceRequest.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> unbindFromServiceResult = applicationsOperations.unbindFromService("someApplication", "someService");

        // then
        assertThat(unbindFromServiceResult, is(notNullValue()));

        UnbindServiceInstanceRequest unbindFromServiceRequest = UnbindServiceInstanceRequest
                .builder()
                .applicationName("someApplication")
                .serviceInstanceName("someService")
                .build();
        verify(servicesMock, times(1)).unbind(unbindFromServiceRequest);
    }

    @Test
    public void testUnbindAppWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.unbindFromService(null, "someService"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.unbindFromService("someApp", null));
    }

    @Test
    public void testAddRouteToAppSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Routes routesMock = mock(Routes.class);
        when(cfOperationsMock.routes()).thenReturn(routesMock);
        when(routesMock.map(any(MapRouteRequest.class))).thenReturn(Mono.just(2));

        Domains domainMock = mock(Domains.class);
        when(cfOperationsMock.domains()).thenReturn(domainMock);
        when(domainMock.list()).thenReturn(Flux.just(Domain.builder()
                .id("domainId")
                .status(Status.OWNED)
                .name("cfapps.io").build()));


        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> addRouteResult = applicationsOperations.addRoute("someApplication", "testRoute.cfapps.io");
        addRouteResult.block();

        // then
        assertThat(addRouteResult, notNullValue());

        MapRouteRequest mapRouteRequest = MapRouteRequest
                .builder()
                .applicationName("someApplication")
                .domain("cfapps.io")
                .host("testRoute")
                .build();
        verify(domainMock, times(1)).list();
        verify(routesMock, times(1)).map(mapRouteRequest);
    }

    @Test
    public void testAddRouteToAppWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.addRoute(null, "someDomain"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.addRoute("someApp", null));
    }

    @Test
    public void testRemoveRouteFromAppSucceeds() {
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Routes routesMock = mock(Routes.class);
        when(cfOperationsMock.routes()).thenReturn(routesMock);
        when(routesMock.unmap(any(UnmapRouteRequest.class))).thenReturn(Mono.just(mock(Void.class)));

        Domains domainMock = mock(Domains.class);
        when(cfOperationsMock.domains()).thenReturn(domainMock);
        when(domainMock.list()).thenReturn(Flux.just(Domain.builder()
                .id("domainId")
                .status(Status.OWNED)
                .name("cfapps.io").build()));

        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(cfOperationsMock);

        // when
        Mono<Void> addRouteResult = applicationsOperations.removeRoute("someApplication", "test.cfapps.io");
        addRouteResult.block();

        // then
        assertThat(addRouteResult, is(notNullValue()));

        UnmapRouteRequest unmapRouteRequest = UnmapRouteRequest
                .builder()
                .applicationName("someApplication")
                .domain("cfapps.io")
                .host("test")
                .build();
        verify(routesMock, times(1)).unmap(unmapRouteRequest);
    }

    @Test
    public void testRemoveRouteFromAppWithNullValuesAsArgumentsThrowsNullPointerException() {
        //given
        DefaultApplicationsOperations applicationsOperations = new DefaultApplicationsOperations(
                mock(DefaultCloudFoundryOperations.class));

        //when + then
        assertThrows(NullPointerException.class, () ->
                applicationsOperations.removeRoute(null, "someDomain"));

        assertThrows(NullPointerException.class, () ->
                applicationsOperations.removeRoute("someApp", null));
    }

    /**
     * Creates an {@link ApplicationManifest} with partially random data to increase
     * test reliability.
     *
     * @return application manifest containing test data
     */
    // FIXME: randomize some data
    private ApplicationManifest createMockApplicationManifest() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("key", "value");

        // note: here we have to insert a path, too!
        // another note: routes and hosts cannot both be set, so we settle with hosts
        // yet another note: docker image and buildpack cannot both be set, so we settle with buildpack

        return ApplicationManifest.builder()
            .buildpack("test_buildpack")
            .command("test command")
            .disk(1234)
            .environmentVariables(envVars)
            .healthCheckHttpEndpoint("http://healthcheck.local")
            .healthCheckType(ApplicationHealthCheck.HTTP)
            .instances(42)
            .memory(Integer.MAX_VALUE)
            .name("notyetrandomname")
            .noRoute(false)
            .path(Paths.get("/test/uri"))
            .randomRoute(true)
            .routes(Route.builder().route("route1").build(),
                Route.builder().route("route2").build())
            .services("servicealpha", "serviceomega")
            .stack("nope")
            .timeout(987654321)
            .build();
    }

    private DefaultCloudFoundryOperations getCloudFoundryOperationsMock(Map<String, ApplicationManifest> apps,
                                                                        Map<String, Metadata> metadata,
                                                                        Throwable pushAppError) {
        ApplicationsV3 applicationsV3Mock = ApplicationsV3MockBuilder
                .get()
                .setMetadata(metadata)
                .build();
        CloudFoundryClient cloudFoundryClientMock = CloudFoundryClientMockBuilder
                .get()
                .setApplicationsV3(applicationsV3Mock)
                .build();
        Applications applicationsMock = ApplicationsMockBuilder
                .get()
                .setApps(apps)
                .setPushApplicationManifestError(pushAppError)
                .build();
        return DefaultCloudFoundryOperationsMockBuilder
                .get()
                .setApplications(applicationsMock)
                .setCloudFoundryClient(cloudFoundryClientMock)
                .build();
    }

}
