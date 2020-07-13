package cloud.foundry.cli.logic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;

import cloud.foundry.cli.mocking.ApplicationsMockBuilder;
import cloud.foundry.cli.mocking.ApplicationsV3MockBuilder;
import cloud.foundry.cli.mocking.CloudFoundryClientMockBuilder;
import cloud.foundry.cli.mocking.DefaultCloudFoundryOperationsMockBuilder;
import cloud.foundry.cli.operations.SpaceOperations;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.CloudFoundryClient;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.*;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Test for {@link ApplyLogic}
 */
public class ApplyLogicTest {

    private static final String METADATA_KEY = "CF_METADATA_KEY";

    @Test
    public void testApplyApplicationsWithNull() {
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        assertThrows(NullPointerException.class, () -> applyLogic.applyApplications(null));
    }

    @Test
    public void testApplySpaceDevelopersWithNull() {
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        assertThrows(NullPointerException.class, () -> applyLogic.applySpaceDevelopers(null));
    }

    @Test
    public void testApplySpaceDevelopersAssignAndRemoveSpaceDevelopers() {
        // given
        // listSpacedevs to apply
        List<String> spaceDevelopersToApply = new LinkedList<>();
        spaceDevelopersToApply.add("Mr. Bean");
        spaceDevelopersToApply.add("toAdd");

        // list live
        List<String> spaceDevelopersLive = new LinkedList<>();
        spaceDevelopersLive.add("Mr. Bean");
        spaceDevelopersLive.add("toDelete");

        DefaultCloudFoundryOperations cfOperationsMock = mockSpaceDevelopersGetAll(spaceDevelopersLive);
        CloudFoundryClient cloudFoundryClientMock = mock(CloudFoundryClient.class);
        when(cfOperationsMock.getCloudFoundryClient()).thenReturn(cloudFoundryClientMock);
        Spaces spacesMock = mock(Spaces.class);
        when(cloudFoundryClientMock.spaces()).thenReturn(spacesMock);

        // assign
        AtomicReference<AssociateSpaceDeveloperByUsernameRequest> assignmentRequest = assignSpaceDevelopersMock(
                spacesMock);

        // delete
        AtomicReference<RemoveSpaceDeveloperByUsernameRequest> removalRequest = deleteSpaceDevelopersMock(spacesMock);

        ApplyLogic applyLogic = new ApplyLogic(cfOperationsMock);

        // when
        applyLogic.applySpaceDevelopers(spaceDevelopersToApply);

        // then
        verify(spacesMock, times(1)).removeDeveloperByUsername(any(RemoveSpaceDeveloperByUsernameRequest.class));
        verify(spacesMock, times(1)).associateDeveloperByUsername(any(AssociateSpaceDeveloperByUsernameRequest.class));

        AssociateSpaceDeveloperByUsernameRequest assignrequest = assignmentRequest.get();
        assertThat(assignrequest, notNullValue());
        assertThat(assignrequest.getUsername(), is("toAdd"));

        RemoveSpaceDeveloperByUsernameRequest deleterequest = removalRequest.get();
        assertThat(deleterequest, notNullValue());
        assertThat(deleterequest.getUsername(), is("toDelete"));

    }

    private DefaultCloudFoundryOperations mockSpaceDevelopersGetAll(List<String> spaceDevelopersLive) {
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);

        when(cfOperationsMock.getSpace()).thenReturn("spaceName");
        when(cfOperationsMock.getOrganization()).thenReturn("organizationName");
        Mono<String> monoMock = mock(Mono.class);
        when(cfOperationsMock.getSpaceId()).thenReturn(monoMock);
        when(monoMock.block()).thenReturn("spaceID");

        UserAdmin userAdminMock = mock(UserAdmin.class);
        when(cfOperationsMock.userAdmin()).thenReturn(userAdminMock);

        AtomicReference<ListSpaceUsersRequest> listingRequest = new AtomicReference<>(null);
        when(userAdminMock.listSpaceUsers(any(ListSpaceUsersRequest.class)))
                .then(invocation -> {
                    listingRequest.set(invocation.getArgument(0));
                    return Mono.just(spaceDevelopersLive)
                            .map(list -> SpaceUsers.builder().addAllDevelopers(list).build());
                });

        return cfOperationsMock;
    }

    // mock for assign space devs
    private AtomicReference<AssociateSpaceDeveloperByUsernameRequest> assignSpaceDevelopersMock(Spaces spacesMock) {
        Mono<Void> assignMonoMock = mock(Mono.class);
        AtomicReference<AssociateSpaceDeveloperByUsernameRequest> assignmentRequest = new AtomicReference<>(null);
        when(spacesMock.associateDeveloperByUsername(any(AssociateSpaceDeveloperByUsernameRequest.class)))
                .then(invocation -> {
                    assignmentRequest.set(invocation.getArgument(0));
                    return Mono.empty();
                });

        when(assignMonoMock.onErrorContinue(any(Predicate.class), any())).thenReturn(assignMonoMock);
        when(assignMonoMock.block()).thenReturn(null);

        return assignmentRequest;
    }

    // mock for delete space devs
    private AtomicReference<RemoveSpaceDeveloperByUsernameRequest> deleteSpaceDevelopersMock(Spaces spacesMock) {
        Mono<Void> deleteMonoMock = mock(Mono.class);
        AtomicReference<RemoveSpaceDeveloperByUsernameRequest> removalRequest = new AtomicReference<>(null);
        when(spacesMock.removeDeveloperByUsername(any(RemoveSpaceDeveloperByUsernameRequest.class)))
                .then(invocation -> {
                    removalRequest.set(invocation.getArgument(0));
                    return Mono.empty();
                });

        when(deleteMonoMock.onErrorContinue(any(Predicate.class), any())).thenReturn(deleteMonoMock);
        when(deleteMonoMock.block()).thenReturn(null);

        return removalRequest;
    }

    @Test
    public void testApplyApplicationsCreatesApplication() {
        ApplicationManifest appManifest = createExampleApplicationManifest("someApplicationName",
                "/some/path",
                "someBuildpack");
        Metadata appMetadata = createMockMetadata("someApplicationName", "some/path");

        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations2(
                Collections.singletonMap("someApplicationName", appManifest),
                Collections.singletonMap("someApplicationName", appMetadata));

        // from now on: setup application to apply
        Map<String, ApplicationBean> applicationsToApply = createDesiredApplications("someApplicationName",
            "/some/path",
            "someBuildpack",
            "app1meta");

        ApplyLogic applyLogic = new ApplyLogic(cfMock);

        // when
        applyLogic.applyApplications(applicationsToApply);

        // then
        verify(cfMock.applications()).list();
        PushApplicationManifestRequest request = PushApplicationManifestRequest
                .builder()
                .manifest(appManifest)
                .noStart(true)
                .build();
        verify(cfMock.applications(), times(1)).pushManifest(request);
    }


    @Test
    public void testApplyApplicationsWithoutDifference() {
        // given
        Map<String, ApplicationBean> appsToApply = createDesiredApplications("app1",
                "path",
                "someBuildpack",
                "app1meta");

        // mock-setup for ApplicationOperations.getAll() delivers 1 application
        ApplicationManifest appManifest = createExampleApplicationManifest("app1", "path", "someBuildpack");
        Metadata appMetadata = createMockMetadata("app1meta", "path");

        DefaultCloudFoundryOperations cfOperationsMock = createMockCloudFoundryOperations2(
                Collections.singletonMap("app1", appManifest),
                Collections.singletonMap("app1", appMetadata)
        );

        ApplyLogic applyLogic = new ApplyLogic(cfOperationsMock);
        //when
        applyLogic.applyApplications(appsToApply);

        // then
        verify(cfOperationsMock.applications()).list();
        verify(cfOperationsMock.applications(), times(0)).delete(any(DeleteApplicationRequest.class));
        verify(cfOperationsMock.applications(), times(0)).pushManifest(any(PushApplicationManifestRequest.class));
    }

    @Test
    public void testApplyApplicationsRemovesApplication() {
        // given
        Map<String, ApplicationBean> appsToApply = createDesiredApplications("app1",
                "/some/path",
                "someBuildpack",
                "app1meta");

        // mock-setup for ApplicationOperations.getAll() delivers 3 applications (app1, app2, app3)
        ApplicationManifest appManifest1 = createExampleApplicationManifest("app1", "/some/path", "someBuildpack");
        Metadata app1Metadata = createMockMetadata("app1meta", "some/path");
        ApplicationManifest appManifest2 = createExampleApplicationManifest("app2", "/some/path", "someBuildpack");
        Metadata app2Metadata = createMockMetadata("app2meta", "some/path");
        ApplicationManifest appManifest3 = createExampleApplicationManifest("app3", "/some/path", "someBuildpack");
        Metadata app3Metadata = createMockMetadata("app3meta", "some/path");

        Map<String, ApplicationManifest> apps = new HashMap<String, ApplicationManifest>() {{
            put("app1", appManifest1);
            put("app2", appManifest2);
            put("app3", appManifest3);
        }};
        Map<String, Metadata> metadata = new HashMap<String, Metadata>() {{
            put("app1", app1Metadata);
            put("app2", app2Metadata);
            put("app3", app3Metadata);
        }};

        DefaultCloudFoundryOperations cfOperationsMock = createMockCloudFoundryOperations2(apps, metadata);

        ApplyLogic applyLogic = new ApplyLogic(cfOperationsMock);

        // when
        applyLogic.applyApplications(appsToApply);

        // then
        DeleteApplicationRequest request1 = DeleteApplicationRequest.builder().name("app2").build();
        verify(cfOperationsMock.applications(), times(1)).delete(request1);
        DeleteApplicationRequest request2 = DeleteApplicationRequest.builder().name("app3").build();
        verify(cfOperationsMock.applications(), times(1)).delete(request2);
        verify(cfOperationsMock.applications()).list();
    }


    private ApplicationManifest createExampleApplicationManifest(String appName, String path, String buildpack) {

        return ApplicationManifest.builder()
                .name(appName)
                .buildpack(buildpack)
                .path(Paths.get(path))
                .instances(1)
                .docker(Docker.builder().build())
                .environmentVariables(Collections.emptyMap())
                .disk(1024)
                .memory(Integer.MAX_VALUE)
                .build();
    }

    private Map<String, ApplicationBean> createDesiredApplications(String appname,
                                                                   String path,
                                                                   String buildpack,
                                                                   String meta) {
        Map<String, ApplicationBean> appconfig = new HashMap<>();
        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setPath(path);
        ApplicationManifestBean manifestBean = new ApplicationManifestBean();
        applicationBean.setManifest(manifestBean);
        manifestBean.setBuildpack(buildpack);
        manifestBean.setInstances(1);
        manifestBean.setDisk(1024);
        manifestBean.setMemory(Integer.MAX_VALUE);
        applicationBean.setMeta(meta);

        appconfig.put(appname, applicationBean);

        return appconfig;
    }

  private DefaultCloudFoundryOperations createMockCloudFoundryOperations2(Map<String, ApplicationManifest> apps,
                                                                          Map<String, Metadata> metadata) {
      Applications applicationsMock = ApplicationsMockBuilder
              .get()
              .setApps(apps)
              .build();
      ApplicationsV3 applicationsV3Mock = ApplicationsV3MockBuilder
              .get()
              .setMetadata(metadata)
              .build();
      CloudFoundryClient cloudFoundryClientMock = CloudFoundryClientMockBuilder
              .get()
              .setApplicationsV3(applicationsV3Mock)
              .build();
      DefaultCloudFoundryOperations cfOperationsMock = DefaultCloudFoundryOperationsMockBuilder
              .get()
              .setApplications(applicationsMock)
              .setCloudFoundryClient(cloudFoundryClientMock)
              .build();

      return cfOperationsMock;
    }

    /**
     * Creates a {@link Metadata metadata instance} for testing purposes.
     *
     * @return metadata for an application
     */
    private Metadata createMockMetadata(String meta, String path) {
        return Metadata.builder()
                .annotation(ApplicationBean.METADATA_KEY, meta)
                .annotation(ApplicationBean.PATH_KEY, path)
                .build();
    }

    @Test
    public void testApplyServicesWithNull() {
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        assertThrows(NullPointerException.class, () -> applyLogic.applyServices(null));
    }

    @Test
    public void testApplyServices() {
        //given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);

        //desired Services
        HashMap<String, ServiceBean> desiredServices = new HashMap<>();
        ServiceBean serviceBean = new ServiceBean();
        desiredServices.put("exampleService", serviceBean);

        //liveConfig
        HashMap<String, ServiceBean> liveConfig = new HashMap<>();
        GetLogic getlogic = mock(GetLogic.class);
        when(getlogic.getServices(any())).thenReturn(liveConfig);

        //Diffresult
        DiffLogic diffLogicMock = mock(DiffLogic.class);
        DiffResult diffResultMock = mock(DiffResult.class);
        when(diffLogicMock.createDiffResult(any(), any())).thenReturn(diffResultMock);

        //allServiceChanges
        List<CfChange> cfChanges = new LinkedList<>();
        CfChange cfChangeMock = mock(CfChange.class);
        cfChanges.add(cfChangeMock);
        Map<String, List<CfChange>> allServiceChanges = mock(Map.class);
        when(diffResultMock.getServiceChanges()).thenReturn(allServiceChanges);
        //entry set of allServiceChanges
        Set<Map.Entry<String, List<CfChange>>> entrySet = new HashSet<>();
        Map.Entry entry = mock(Map.Entry.class);
        entrySet.add(entry);
        when(allServiceChanges.entrySet()).thenReturn(entrySet);


        //when
        ApplyLogic applyLogic = new ApplyLogic(cfOperationsMock);
        applyLogic.setDiffLogic(diffLogicMock);
        applyLogic.setGetLogic(getlogic);
        applyLogic.applyServices(desiredServices);

        //then
        verify(getlogic).getServices(any());
        verify(diffLogicMock).createDiffResult(any(), any());
        //this is called when you apply to the changes
        verify(entry).getKey();
        verify(entry).getValue();
    }

    @Test
    public void testApplySpaceCreatesSpace() {

        // given
        String desiredSpaceName = "testName";
        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);

        List<String> presentSpaces = Arrays.asList("space1", "space2");
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(presentSpaces));

        Mono<Void> resultingMono = mock(Mono.class);
        when(spaceOperationsMock.create(desiredSpaceName)).thenReturn(resultingMono);

        // the constructor paramteres won't be used by apply space method, because it uses
        // dependency injection regarding space operations
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        // when
        applyLogic.applySpace(desiredSpaceName, spaceOperationsMock);

        // then
        verify(resultingMono).block();
    }

    @Test
    public void testApplySpaceWithSpaceAlreadyExisting() {
        // given
        String desiredSpaceName = "testName";
        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);

        List<String> presentSpaces = Arrays.asList("testName", "otherSpace");
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(presentSpaces));

        Mono<Void> resultingMono = mock(Mono.class);
        when(spaceOperationsMock.create(desiredSpaceName)).thenReturn(resultingMono);

        // the constructor paramteres won't be used by apply space method, because it uses DI
        // regarding space operations.
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        // when
        applyLogic.applySpace(desiredSpaceName, spaceOperationsMock);

        // then
        verify(resultingMono, never()).block();
    }

    @Test
    public void testApplySpaceWithGetSpaceNamesFailingThrowsGetException() {
        // given
        String desiredSpaceName = "testName";
        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);

        Mono<List<String>> getRequestMock = mock(Mono.class);
        when(spaceOperationsMock.getAll()).thenReturn(getRequestMock);
        when(getRequestMock.block()).thenThrow(new RuntimeException("Get Space Names Failing"));

        // the constructor paramteres won't be used by apply space method, because it uses DI
        // regarding space operations.
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        // when + then
        assertThrows(GetException.class, () ->
                applyLogic.applySpace(desiredSpaceName, spaceOperationsMock));
    }

    @Test
    public void testApplySpaceWithCreateSpaceFailingThrowsApplyException() {

        // given
        String desiredSpaceName = "testName";
        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);

        List<String> presentSpaces = Arrays.asList("space1", "space2");
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(presentSpaces));

        Mono<Void> resultingMono = mock(Mono.class);
        when(spaceOperationsMock.create(desiredSpaceName)).thenReturn(resultingMono);
        when(resultingMono.block()).thenThrow(new RuntimeException("Create space failing"));

        // the constructor paramteres won't be used by apply space method, because it uses DI
        // regarding space operations.
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
         
         // when + then
        assertThrows(ApplyException.class, () ->
                applyLogic.applySpace(desiredSpaceName, spaceOperationsMock));
    }

    @Test
    public void testApplySpaceWithNullValuesAsArgumentsThrowsNullPointerException() {
        // given
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);
        
        // when + then
        assertThrows(NullPointerException.class, () ->
                applyLogic.applySpace(null, spaceOperationsMock));

        assertThrows(NullPointerException.class, () ->
                applyLogic.applySpace("testName", null));
    }

}
