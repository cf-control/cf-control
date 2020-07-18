package cloud.foundry.cli.logic;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.GetException;
import cloud.foundry.cli.crosscutting.mapping.beans.*;

import cloud.foundry.cli.operations.*;
import cloud.foundry.cli.services.LoginCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Test for {@link ApplyLogic}
 */
public class ApplyLogicTest {

    @Test
    public void testConstructorOnNullParameterThrowsException() {
        assertThrows(NullPointerException.class, () -> new ApplyLogic(null));
    }

    @Test
    public void testApplyAllOnNullParametersThrowsException() {
        ApplyLogic applyLogic =  new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
        assertThrows(NullPointerException.class, () -> applyLogic.applyAll(null, new LoginCommandOptions()));
        assertThrows(NullPointerException.class, () -> applyLogic.applyAll(new ConfigBean(), null));
    }

    @Test
    public void testApplyAllNothingToApply() {
        // given
        ConfigBean configBean = new ConfigBean();
        configBean.setSpec(new SpecBean());
        configBean.setTarget(new TargetBean());

        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getAll(any(),any(), any(), any(), any())).thenReturn(configBean);

        applyLogic.setGetLogic(getLogicMock);

        // when
        applyLogic.applyAll(configBean, new LoginCommandOptions());

        // then
        verify(getLogicMock, times(1)).getAll(any(SpaceDevelopersOperations.class),
                any(ServicesOperations.class),
                any(ApplicationsOperations.class),
                any(ClientOperations.class),
                any(LoginCommandOptions.class));
    }

    @Test
    public void testApplyAllWithDifference() {
        // given
        // create the live config
        ConfigBean liveConfigBean = new ConfigBean();
        liveConfigBean.setSpec(new SpecBean());
        liveConfigBean.setTarget(new TargetBean());

        // create the desired config
        ConfigBean desiredConfigBean = new ConfigBean();

        SpecBean desiredSpecBean = new SpecBean();
        desiredSpecBean.setSpaceDevelopers(singletonList("spaceDeveloper1"));

        ServiceBean desiredServiceBean = new ServiceBean();
        desiredServiceBean.setService("sqlservice");
        desiredSpecBean.setServices(singletonMap("service", desiredServiceBean));

        ApplicationBean desiredApplicationBean = new ApplicationBean();
        desiredApplicationBean.setPath("some/path");
        desiredSpecBean.setApps(singletonMap("app", desiredApplicationBean));

        TargetBean desiredTargetBean = new TargetBean();
        desiredTargetBean.setSpace("space");
        desiredConfigBean.setSpec(desiredSpecBean);
        desiredConfigBean.setTarget(desiredTargetBean);

        // mock get logic
        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getAll(any(),any(), any(), any(), any())).thenReturn(liveConfigBean);

        // mock space developers operations
        SpaceDevelopersOperations spaceDevelopersOperations = mock(SpaceDevelopersOperations.class);
        when(spaceDevelopersOperations.getSpaceId()).thenReturn(Mono.just("spaceId"));
        when(spaceDevelopersOperations.assign(anyString(), anyString())).thenReturn(Mono.empty());

        // mock applications operations
        ApplicationsOperations applicationsOperations = mock(ApplicationsOperations.class);
        when(applicationsOperations.create(anyString(), any(), anyBoolean())).thenReturn(Mono.empty());

        // mock services operations
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        when(servicesOperations.create(anyString(), any())).thenReturn(Mono.empty());

        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        applyLogic.setGetLogic(getLogicMock);
        applyLogic.setSpaceDevelopersOperations(spaceDevelopersOperations);
        applyLogic.setApplicationsOperations(applicationsOperations);
        applyLogic.setServicesOperations(servicesOperations);

        // when
        applyLogic.applyAll(desiredConfigBean, new LoginCommandOptions());

        // then
        verify(getLogicMock, times(1)).getAll(any(SpaceDevelopersOperations.class),
                any(ServicesOperations.class),
                any(ApplicationsOperations.class),
                any(ClientOperations.class),
                any(LoginCommandOptions.class));
        verify(spaceDevelopersOperations, times(1)).getSpaceId();
        verify(spaceDevelopersOperations, times(1)).assign("spaceDeveloper1", "spaceId");
        verify(applicationsOperations, times(1)).create(eq("app"), any(ApplicationBean.class), anyBoolean());
        verify(servicesOperations, times(1)).create(eq("service"), any(ServiceBean.class));
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

        // the constructor parameters won't be used by apply space method, because it uses
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

        // the constructor parameters won't be used by apply space method, because it uses DI
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
