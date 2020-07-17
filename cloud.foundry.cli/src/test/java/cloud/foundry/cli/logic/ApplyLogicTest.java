package cloud.foundry.cli.logic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

  private DefaultCloudFoundryOperations createMockCloudFoundryOperations(Map<String, ApplicationManifest> apps,
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
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when
        applyLogic.applySpace(desiredSpaceName);

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
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when
        applyLogic.applySpace(desiredSpaceName);

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

        // the constructor parameters won't be used by apply space method, because it uses DI
        // regarding space operations.
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when + then
        assertThrows(GetException.class, () ->
                applyLogic.applySpace(desiredSpaceName));
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

        // the constructor parameters won't be used by apply space method, because it uses DI
        // regarding space operations.
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when + then
        assertThrows(ApplyException.class, () ->
                applyLogic.applySpace(desiredSpaceName));
    }

    @Test
    public void testApplySpaceWithNullValuesAsArgumentsThrowsNullPointerException() {
        // given
        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when + then
        assertThrows(NullPointerException.class, () ->
                applyLogic.applySpace(null));
    }

}
