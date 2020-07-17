package cloud.foundry.cli.logic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;

import cloud.foundry.cli.operations.*;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.DiffResult;
import cloud.foundry.cli.logic.diff.change.CfChange;

import org.cloudfoundry.operations.applications.*;
import org.cloudfoundry.client.v3.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.*;

/**
 * Test for {@link ApplyLogic}
 */
public class ApplyLogicTest {

    private ApplyLogic applyLogic;
    private OperationsFactory operationsFactory;
    private ApplicationsOperations applicationsOperationsMock;
    private ServicesOperations servicesOperationsMock;
    private SpaceDevelopersOperations spaceDevelopersOperationsMock;
    private SpaceOperations spaceOperationsMock;
    private ClientOperations clientOperations;

    @BeforeEach
    void setup() {
        this.applicationsOperationsMock = mock(ApplicationsOperations.class);
        this.servicesOperationsMock = mock(ServicesOperations.class);
        this.spaceDevelopersOperationsMock = mock(SpaceDevelopersOperations.class);
        this.spaceOperationsMock = mock(SpaceOperations.class);
        this.clientOperations = mock(ClientOperations.class);
        this.operationsFactory = mock(OperationsFactory.class);

        when(operationsFactory.createApplicationsOperations()).thenReturn(applicationsOperationsMock);
        when(operationsFactory.createServiceOperations()).thenReturn(servicesOperationsMock);
        when(operationsFactory.createSpaceDevelopersOperations()).thenReturn(spaceDevelopersOperationsMock);
        when(operationsFactory.createSpaceOperations()).thenReturn(spaceOperationsMock);
        when(operationsFactory.createClientOperations()).thenReturn(clientOperations);

        applyLogic = new ApplyLogic(operationsFactory);
    }

    @Test
    public void testApplyApplicationsWithNull() {
        assertThrows(NullPointerException.class, () -> applyLogic.applyApplications(null));
    }

    @Test
    public void testApplySpaceDevelopersWithNull() {
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

        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getSpaceDevelopers())
                .thenReturn(spaceDevelopersLive);

        when(spaceDevelopersOperationsMock.getSpaceId())
                .thenReturn(Mono.just("spaceId"));
        when(spaceDevelopersOperationsMock.assign(anyString(), anyString()))
                .thenReturn(Mono.just(mock(Void.class)));
        when(spaceDevelopersOperationsMock.remove(anyString(), anyString()))
                .thenReturn(Mono.just(mock(Void.class)));

        applyLogic.setGetLogic(getLogicMock);

        // when
        applyLogic.applySpaceDevelopers(spaceDevelopersToApply);

        // then
        verify(getLogicMock, times(1)).getSpaceDevelopers();
        verify(spaceDevelopersOperationsMock, times(1))
                .getSpaceId();
        verify(spaceDevelopersOperationsMock, times(1))
                .assign(anyString(), anyString());
        verify(spaceDevelopersOperationsMock, times(1))
                .remove(anyString(), anyString());
    }

    @Test
    public void testApplyApplicationsCreatesApplication() {
        ApplicationManifest appManifest = createExampleApplicationManifest("someApplicationName",
                "/some/path",
                "someBuildpack");
        Metadata appMetadata = createMockMetadata("someApplicationName", "some/path");

        Map<String, ApplicationBean> appsOnLiveSystem = Collections.singletonMap("someApplicationName",
                new ApplicationBean(appManifest, appMetadata));

        // from now on: setup application to apply
        Map<String, ApplicationBean> applicationsToApply = createDesiredApplications("otherApplication",
            "/some/path",
            "someBuildpack",
            "app1meta");

        when(applicationsOperationsMock.create(anyString(), any(ApplicationBean.class), anyBoolean()))
                .thenReturn(Mono.just(mock(Void.class)));

        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getApplications())
                .thenReturn(appsOnLiveSystem);

        applyLogic.setGetLogic(getLogicMock);

        // when
        applyLogic.applyApplications(applicationsToApply);

        // then
        verify(getLogicMock, times(1)).getApplications();
        verify(applicationsOperationsMock, times(1))
                .create(eq("otherApplication"), any(ApplicationBean.class), anyBoolean());
    }


    @Test
    public void testApplyApplicationsWithoutDifference() {
        // given
        Map<String, ApplicationBean> appsToApply = createDesiredApplications("app1",
                "path",
                "someBuildpack",
                "app1meta");

        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getApplications()).thenReturn(appsToApply);

        applyLogic.setGetLogic(getLogicMock);

        //when
        applyLogic.applyApplications(appsToApply);

        // then
        // then
        verify(getLogicMock, times(1)).getApplications();
        verifyNoMoreInteractions(applicationsOperationsMock);
    }

    @Test
    public void testApplyApplicationsDoesntApplyWhenThereAreNoApplications() {
        // given
        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getApplications()).thenReturn(Collections.emptyMap());

        applyLogic.setGetLogic(getLogicMock);

        // when
        applyLogic.applyApplications(Collections.emptyMap());

        // then
        verify(getLogicMock, times(1)).getApplications();
        verifyNoMoreInteractions(applicationsOperationsMock);
    }

    @Test
    public void testApplyApplicationsAppliesChange() {
        // given
        when(applicationsOperationsMock.remove("app1"))
                .thenReturn(Mono.empty());

        Map<String, ApplicationBean> appsToApply = createDesiredApplications("app1",
                "/some/path",
                "someBuildpack",
                "app1meta");
        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getApplications()).thenReturn(Collections.emptyMap());

        applyLogic.setGetLogic(getLogicMock);

        // when
        applyLogic.applyApplications(appsToApply);

        // then
        verify(getLogicMock, times(1)).getApplications();
        verify(applicationsOperationsMock, times(1)).create("app1",appsToApply.get("app1"), false);
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
        assertThrows(NullPointerException.class, () -> applyLogic.applyServices(null));
    }

    @Test
    public void testApplyServices() {
        //given
        //desired Services
        HashMap<String, ServiceBean> desiredServices = new HashMap<>();
        ServiceBean serviceBean = new ServiceBean();
        desiredServices.put("exampleService", serviceBean);

        //liveConfig
        HashMap<String, ServiceBean> liveConfig = new HashMap<>();
        GetLogic getlogic = mock(GetLogic.class);
        when(getlogic.getServices()).thenReturn(liveConfig);

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
        applyLogic.setDiffLogic(diffLogicMock);
        applyLogic.setGetLogic(getlogic);
        applyLogic.applyServices(desiredServices);

        //then
        verify(getlogic).getServices();
        verify(diffLogicMock).createDiffResult(any(), any());
        //this is called when you apply to the changes
        verify(entry).getKey();
        verify(entry).getValue();
    }

    @Test
    public void testApplySpaceCreatesSpace() {

        // given
        String desiredSpaceName = "testName";

        List<String> presentSpaces = Arrays.asList("space1", "space2");
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(presentSpaces));

        Mono<Void> resultingMono = mock(Mono.class);
        when(spaceOperationsMock.create(desiredSpaceName)).thenReturn(resultingMono);

        // when
        applyLogic.applySpace(desiredSpaceName);

        // then
        verify(resultingMono).block();
    }

    @Test
    public void testApplySpaceWithSpaceAlreadyExisting() {
        // given
        String desiredSpaceName = "testName";

        List<String> presentSpaces = Arrays.asList("testName", "otherSpace");
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(presentSpaces));

        Mono<Void> resultingMono = mock(Mono.class);
        when(spaceOperationsMock.create(desiredSpaceName)).thenReturn(resultingMono);

        // when
        applyLogic.applySpace(desiredSpaceName);

        // then
        verify(resultingMono, never()).block();
    }

    @Test
    public void testApplySpaceWithGetSpaceNamesFailingThrowsGetException() {
        // given
        String desiredSpaceName = "testName";

        Mono<List<String>> getRequestMock = mock(Mono.class);
        when(spaceOperationsMock.getAll()).thenReturn(getRequestMock);
        when(getRequestMock.block()).thenThrow(new RuntimeException("Get Space Names Failing"));

        // when + then
        assertThrows(GetException.class, () ->
                applyLogic.applySpace(desiredSpaceName));
    }

    @Test
    public void testApplySpaceWithCreateSpaceFailingThrowsApplyException() {

        // given
        String desiredSpaceName = "testName";

        List<String> presentSpaces = Arrays.asList("space1", "space2");
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(presentSpaces));

        Mono<Void> resultingMono = mock(Mono.class);
        when(spaceOperationsMock.create(desiredSpaceName)).thenReturn(resultingMono);
        when(resultingMono.block()).thenThrow(new RuntimeException("Create space failing"));

        // when + then
        assertThrows(ApplyException.class, () ->
                applyLogic.applySpace(desiredSpaceName));
    }

    @Test
    public void testApplySpaceWithNullValuesAsArgumentsThrowsNullPointerException() {
        // when + then
        assertThrows(NullPointerException.class, () ->
                applyLogic.applySpace(null));
    }

}
