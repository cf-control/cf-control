package cloud.foundry.cli.logic;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    public void testApplyOnNullParametersThrowsException() {
        ApplyLogic applyLogic =  new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
        assertThrows(NullPointerException.class, () -> applyLogic.apply(null, new LoginCommandOptions()));
        assertThrows(NullPointerException.class, () -> applyLogic.apply(new ConfigBean(), null));
    }


    @Test
    public void testApplyOnNulLTargetThrowsException() {
        ApplyLogic applyLogic =  new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
        ConfigBean desiredConfig = new ConfigBean();

        assertThrows(NullPointerException.class, () -> applyLogic.apply(desiredConfig, new LoginCommandOptions()));
    }

    @Test
    public void testApplyOnNullSpaceThrowsException() {
        ApplyLogic applyLogic =  new ApplyLogic(mock(DefaultCloudFoundryOperations.class));
        ConfigBean desiredConfig = new ConfigBean();
        desiredConfig.setTarget(new TargetBean());

        assertThrows(NullPointerException.class, () -> applyLogic.apply(new ConfigBean(), null));
    }

    @Test
    public void testApplyNothingToApply() {
        // given
        ConfigBean desiredConfigBean = new ConfigBean();
        desiredConfigBean.setSpec(new SpecBean());
        TargetBean desiredTargetBean = new TargetBean();
        desiredTargetBean.setSpace("space");
        desiredConfigBean.setTarget(desiredTargetBean);

        DefaultCloudFoundryOperations dcfoMock = mock(DefaultCloudFoundryOperations.class);
        ApplyLogic applyLogic = new ApplyLogic(dcfoMock);

        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getAll(any(),any(), any(), any(), any())).thenReturn(desiredConfigBean);

        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(Collections.singletonList("space")));

        applyLogic.setGetLogic(getLogicMock);
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when
        applyLogic.apply(desiredConfigBean, new LoginCommandOptions());

        // then
        verify(spaceOperationsMock, times(1)).getAll();
        verify(getLogicMock, times(1)).getAll(any(SpaceDevelopersOperations.class),
                any(ServicesOperations.class),
                any(ApplicationsOperations.class),
                any(ClientOperations.class),
                any(LoginCommandOptions.class));
    }

    @Test
    public void testApplyCreatesSpaceWhenNotExistingAndSkipsGetAll() {
        // given
        ConfigBean desiredConfigBean = new ConfigBean();
        desiredConfigBean.setSpec(new SpecBean());
        TargetBean desiredTargetBean = new TargetBean();
        desiredTargetBean.setSpace("space");
        desiredConfigBean.setTarget(desiredTargetBean);

        DefaultCloudFoundryOperations dcfoMock = mock(DefaultCloudFoundryOperations.class);
        ApplyLogic applyLogic = new ApplyLogic(dcfoMock);

        GetLogic getLogicMock = mock(GetLogic.class);
        when(getLogicMock.getAll(any(),any(), any(), any(), any())).thenReturn(desiredConfigBean);

        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(Collections.emptyList()));
        when(spaceOperationsMock.create(anyString())).thenReturn(Mono.empty());

        applyLogic.setGetLogic(getLogicMock);
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when
        applyLogic.apply(desiredConfigBean, new LoginCommandOptions());

        // then
        verify(spaceOperationsMock, times(1)).getAll();
        verify(spaceOperationsMock, times(1)).create("space");
        verify(getLogicMock, times(0)).getAll(any(SpaceDevelopersOperations.class),
                any(ServicesOperations.class),
                any(ApplicationsOperations.class),
                any(ClientOperations.class),
                any(LoginCommandOptions.class));
    }

    @Test
    public void testApplyWithDifference() {
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
        when(applicationsOperations.create(anyString(), any())).thenReturn(Mono.empty());

        // mock services operations
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        when(servicesOperations.create(anyString(), any())).thenReturn(Mono.empty());

        // mock space operations
        SpaceOperations spaceOperationsMock = mock(SpaceOperations.class);
        when(spaceOperationsMock.getAll()).thenReturn(Mono.just(Collections.singletonList("space")));

        ApplyLogic applyLogic = new ApplyLogic(mock(DefaultCloudFoundryOperations.class));

        applyLogic.setGetLogic(getLogicMock);
        applyLogic.setSpaceDevelopersOperations(spaceDevelopersOperations);
        applyLogic.setApplicationsOperations(applicationsOperations);
        applyLogic.setServicesOperations(servicesOperations);
        applyLogic.setSpaceOperations(spaceOperationsMock);

        // when
        applyLogic.apply(desiredConfigBean, new LoginCommandOptions());

        // then
        verify(spaceOperationsMock, times(1)).getAll();
        verify(getLogicMock, times(1)).getAll(any(SpaceDevelopersOperations.class),
                any(ServicesOperations.class),
                any(ApplicationsOperations.class),
                any(ClientOperations.class),
                any(LoginCommandOptions.class));
        verify(spaceDevelopersOperations, times(1)).getSpaceId();
        verify(spaceDevelopersOperations, times(1)).assign("spaceDeveloper1", "spaceId");
        verify(applicationsOperations, times(1)).create(eq("app"), any(ApplicationBean.class));
        verify(servicesOperations, times(1)).create(eq("service"), any(ServiceBean.class));
    }


}
