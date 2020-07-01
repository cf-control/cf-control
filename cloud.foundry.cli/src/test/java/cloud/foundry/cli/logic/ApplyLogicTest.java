package cloud.foundry.cli.logic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;

import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.v2.spaces.RemoveSpaceDeveloperByUsernameRequest;
import org.cloudfoundry.client.CloudFoundryClient;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.useradmin.ListSpaceUsersRequest;
import org.cloudfoundry.operations.useradmin.SpaceUsers;
import org.cloudfoundry.operations.useradmin.UserAdmin;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Test for {@link ApplyLogic}
 */
public class ApplyLogicTest {

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
        // given
        DefaultCloudFoundryOperations cfOperationsMock = mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = mock(Applications.class);

        // from now on: mock-setup for ApplicationOperations.getAll delivers empty
        // applications
        when(cfOperationsMock.applications()).thenReturn(applicationsMock);
        when(applicationsMock.list()).thenReturn(Flux.empty());

        // from now on: mock-setup for ApplicationOperations.create delivers successful
        // creation
        when(applicationsMock.get(any(GetApplicationRequest.class))).thenThrow(IllegalArgumentException.class);
        Mono<Void> pushManifestMonoMock = mock(Mono.class);

        // this will contain the received PushApplicationManifestRequest when
        // pushManifest is called
        AtomicReference<PushApplicationManifestRequest> receivedPushRequest = new AtomicReference<>(null);

        when(applicationsMock.pushManifest(any(PushApplicationManifestRequest.class)))
                .thenAnswer((Answer<Mono<Void>>) invocation -> {
                    receivedPushRequest.set(invocation.getArgument(0));
                    return pushManifestMonoMock;
                });
        when(pushManifestMonoMock.onErrorContinue(any(Predicate.class), any())).thenReturn(pushManifestMonoMock);
        when(pushManifestMonoMock.block()).thenReturn(null);

        // from now on: setup application to apply
        Map<String, ApplicationBean> applicationsToApply = new HashMap<>();
        String applicationName = "someApplicationName";
        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setPath("/some/path");
        ApplicationManifestBean manifestBean = new ApplicationManifestBean();
        applicationBean.setManifest(manifestBean);
        manifestBean.setBuildpack("someBuildpack");
        applicationsToApply.put(applicationName, applicationBean);

        ApplyLogic applyLogic = new ApplyLogic(cfOperationsMock);

        // when
        applyLogic.applyApplications(applicationsToApply);

        // then
        verify(applicationsMock).list();

        PushApplicationManifestRequest actualReceivedPushRequest = receivedPushRequest.get();
        assertThat(actualReceivedPushRequest, is(notNullValue()));
        assertThat(actualReceivedPushRequest.getManifests().size(), is(1));

        ApplicationManifest manifest = actualReceivedPushRequest.getManifests().get(0);
        assertThat(manifest.getName(), is(applicationName));
        assertThat(manifest.getPath(), is(Paths.get("/some/path")));
        assertThat(manifest.getBuildpack(), is("someBuildpack"));
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
}
