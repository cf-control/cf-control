package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import cloud.foundry.cli.mocking.DefaultCloudFoundryOperationsMockBuilder;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationResponse;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for {@link ApplicationsOperations}
 */
public class ApplicationsOperationsTest {

    private static final String SOME_APPLICATION = "SOME_APPLICATION";

    @Test
    public void testGetApplicationsWithEmptyMockData() {
        // prepare mock CF API client with an empty applications list
        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations(Collections.emptyList(),
            Collections.emptyList(), null);
        DefaultCloudFoundryOperations cfMock = DefaultCloudFoundryOperationsMockBuilder.get().setApplications(Collections.emptyMap()).build();

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfMock);
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
        Metadata metadata = createMockMedatadata();
        ApplicationSummary summary = createMockApplicationSummary(appManifest);

        // now, let's create the mock object from that list
        DefaultCloudFoundryOperations cfMock = DefaultCloudFoundryOperationsMockBuilder
                .get()
                .setApplications(Collections.singletonMap("appId", appManifest))
                .build();
        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations(Arrays.asList(summary),
            Arrays.asList(appManifest), metadata);

        // now, we can generate a YAML doc for our ApplicationSummary
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfMock);

        // when
        Map<String, ApplicationBean> apps = applicationsOperations.getAll().block();

        // then
        // ... and make sure it contains exactly what we'd expect
        assertThat(apps.size(), is(1));
        assertTrue(apps.containsKey("notyetrandomname"));
        ApplicationBean appBean = apps.get("notyetrandomname");
        assertThat(appBean.getPath(), is(Paths.get("/test/uri").toString()));
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
        assertThat(appBean.getManifest().getRoutes().size(), is(2));
        assertThat(appBean.getManifest().getRoutes(), contains("route1", "route2"));
        assertThat(appBean.getManifest().getServices().size(), is(1));
        assertThat(appBean.getManifest().getServices(), contains("serviceomega"));
        assertThat(appBean.getManifest().getStack(), is("nope"));
        assertThat(appBean.getManifest().getTimeout(), is(987654321));
        assertThat(appBean.getMeta(), is("notyetrandomname, 1.0.1, some/branch"));
    }

    @Test
    public void testCreateApplicationsPushesAppManifestSucceeds() throws CreationException {
        // given

        ApplicationManifest appManifest = createMockApplicationManifest();
        DefaultCloudFoundryOperations cfoMock = DefaultCloudFoundryOperationsMockBuilder
                .get()
                .setApplications(Collections.singletonMap("appId", appManifest))
                .build();

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfoMock);

        ApplicationBean applicationsBean = new ApplicationBean(appManifest);
        applicationsBean.setPath("some/path");
        applicationsBean.setMeta("somemeta");

        //when
        Mono<Void> request = applicationsOperations.create(appManifest.getName(), applicationsBean, false);
        request.block();

        // then
        assertThat(request, notNullValue());
        verify(cfoMock.applications(), times(1)).pushManifest(any(PushApplicationManifestRequest.class));
        verify(cfoMock.applications(), times(1)).get(any(GetApplicationRequest.class));
        UpdateApplicationRequest updateRequest = UpdateApplicationRequest
                .builder()
                .applicationId("appId")
                .metadata(Metadata.builder().label("CF_CONTROL_APP_META", applicationsBean.getMeta()).build())
                .build();
        verify(cfoMock.getCloudFoundryClient().applicationsV3(), times(1)).update(updateRequest);
    }

    @Test
    public void testCreateThrowsExceptionWhenPushAppManifestFails() {
        //given
        ApplicationManifest appManifest = createMockApplicationManifest();
        DefaultCloudFoundryOperations cfoMock = DefaultCloudFoundryOperationsMockBuilder
                .get()
                .setApplications(Collections.singletonMap("appId", appManifest))
                .setPushApplicationManifestError(new Exception())
                .build();

        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfoMock);

        ApplicationBean applicationsBean = new ApplicationBean(appManifest);
        applicationsBean.setPath("some/path");
        applicationsBean.setMeta("somemeta");

        //when
        Mono<Void> request = applicationsOperations.create(appManifest.getName(), applicationsBean, false);

        //then
        assertThat(request, notNullValue());
        assertThrows(Exception.class, request::block);
        verify(cfoMock.applications(), times(1)).pushManifest(any(PushApplicationManifestRequest.class));
        verify(cfoMock.applications(), times(0)).get(any(GetApplicationRequest.class));
        verify(cfoMock.getCloudFoundryClient().applicationsV3(), times(0)).update(any(UpdateApplicationRequest.class));
    }

    @Test
    public void testCreateApplicationsOnMissingDockerPasswordThrowsCreationException() {
        //given
        DefaultCloudFoundryOperations cfoMock = DefaultCloudFoundryOperationsMockBuilder.get().build();
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfoMock);

        ApplicationBean applicationsBean = new ApplicationBean();
        ApplicationManifestBean applicationManifestBean = new ApplicationManifestBean();
        applicationManifestBean.setDockerImage("some/image");
        applicationManifestBean.setDockerUsername("username");

        applicationsBean.setManifest(applicationManifestBean);

        // when
        CreationException exception = assertThrows(CreationException.class,
            () -> applicationsOperations.create("appName", applicationsBean, false));
        assertThat(exception.getMessage(), containsString("Docker password is not set"));
    }

    @Test
    public void testCreateOnNullNameThrowsNullPointerException() throws CreationException {
        // given
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(
                DefaultCloudFoundryOperationsMockBuilder.get().build());

        //then
        assertThrows(NullPointerException.class,
                () -> applicationsOperations.create(null, new ApplicationBean(), false));
    }

    @Test
    public void testCreateOnEmptyNameThrowsIllegalArgumentException() {
        // given
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(
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
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(
                DefaultCloudFoundryOperationsMockBuilder.get().build());

        // when
        assertThrows(NullPointerException.class, () -> applicationsOperations.create("appName", null, false));
    }

    @Test
    public void testRemoveApplication() {
        // given
        DefaultCloudFoundryOperations cfoMock = DefaultCloudFoundryOperationsMockBuilder.get().build();
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfoMock);

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
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfoMock);

        // when -> then
        assertThrows(NullPointerException.class, () -> applicationsOperations.remove(null));
    }

    /**
     * Creates an {@link Metadata} for testing purposes.
     *
     * @return metadata for an application
     */
    private Metadata createMockMedatadata() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("name", "notyetrandomname");
        labels.put("version", "1.0.1");
        labels.put("branch", "some/branch");
        labels.put("id", "1234");

        Metadata metadata = mock(Metadata.class);
        when(metadata.getLabels()).thenReturn(labels);

        return metadata;
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
            .services("serviceomega")
            .stack("nope")
            .timeout(987654321)
            .build();
    }

}
