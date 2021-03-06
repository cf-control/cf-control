package cloud.foundry.cli.logic.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.LinkedList;

class ApplicationRequestPlannerTest {

    @Test
    void applyTest_WithMultipleNewObject_AcceptMethodCallOnOnlyOne() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        when(appOperations.create(any(String.class), any(ApplicationBean.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        String appName = "testApp";

        LinkedList<CfChange> cfChanges = new LinkedList<>();
        CfNewObject newObject = mock(CfNewObject.class);
        ApplicationBean applicationBean = new ApplicationBean();
        when(newObject.getAffectedObject())
                .thenReturn(applicationBean);

        CfNewObject newObject2 = mock(CfNewObject.class);

        cfChanges.add(newObject);
        cfChanges.add(newObject2);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).create("testApp", applicationBean);
    }

    @Test
    void applyTest_WithChangeObjectNotAppBeanOrAppManifestBean() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        CfNewObject newObject = new CfNewObject(serviceBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        assertThrows(ApplyException.class,
            () -> requestsPlanner.createApplyRequests(appName, cfChanges));
    }

    @Test
    void applyTest_WithNewChangeObject_AppCreated() throws CreationException {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean appBeanMock = mock(ApplicationBean.class);
        CfNewObject newObject = new CfNewObject(appBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);
        Void voidMock = mock(Void.class);
        Mono<Void> monoMock = Mono.just(voidMock);
        when(appOperations.create(anyString(), any())).thenReturn(monoMock);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        verify(appOperations, times(1)).create(appName, appBeanMock);
        StepVerifier.create(requests)
            .expectNext(voidMock)
            .expectComplete()
            .verify();
    }

    @Test
    void applyTest_WithNewChangeObject_CreationException() throws CreationException {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean appBeanMock = mock(ApplicationBean.class);
        CfNewObject newObject = new CfNewObject(appBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);
        doThrow(new CreationException("Test")).when(appOperations).create(appName, appBeanMock);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        ApplyException applyException = assertThrows(ApplyException.class,
            () -> requestsPlanner.createApplyRequests(appName,  cfChanges));
        // then
        assertThat(applyException.getCause(), is(instanceOf(CreationException.class)));
    }

    @Test
    void applyTest_WithRemovedObject_AppRemoved() throws CreationException {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean appBeanMock = mock(ApplicationBean.class);
        CfRemovedObject removedObject = new CfRemovedObject(appBeanMock, "propertyName", Arrays.asList("path"));
        cfChanges.add(removedObject);

        Void voidMock = mock(Void.class);
        Mono<Void> monoMock = Mono.just(voidMock);
        when(appOperations.remove(appName)).thenReturn(monoMock);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        verify(appOperations, times(1)).remove(appName);
        StepVerifier.create(requests)
            .expectNext(voidMock)
            .expectComplete()
            .verify();
    }

    @Test
    void applyTest_WithRemovedObjectNotAppBeanOrAppManifestBean() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        CfRemovedObject removedObject = new CfRemovedObject(serviceBeanMock, "propertyName", Arrays.asList("path"));
        cfChanges.add(removedObject);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        assertThrows(ApplyException.class,
            () -> requestsPlanner.createApplyRequests(appName, cfChanges));
    }


    @Test
    void applyTest_OnlyScalableField() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        when(appOperations.scale(any(), any(), any(), any()))
                .thenReturn(Mono.just(mock(Void.class)));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();
        ApplicationManifestBean manifestBean = new ApplicationManifestBean();
        manifestBean.setInstances(4);
        applicationBean.setManifest(manifestBean);
        CfObjectValueChanged changedObject = new CfObjectValueChanged(applicationBean,
                "instances",
                Arrays.asList("path"),
                "2",
                "4");
        cfChanges.add(changedObject);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).scale("testApp", null, null, 4);
        verifyNoMoreInteractions(appOperations);
    }

    @Test
    void applyTest_OnlyEnvironmentVariables() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        Void voidMockAdded = mock(Void.class);
        when(appOperations.addEnvironmentVariable(any(), any(), any()))
                .thenReturn(Mono.just(voidMockAdded));
        Void voidMockRemoved = mock(Void.class);
        when(appOperations.removeEnvironmentVariable(any(), any()))
                .thenReturn(Mono.just(voidMockRemoved));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfMapValueChanged mapValueAdded = new CfMapValueChanged("addedKey",
                "",
                "added",
                ChangeType.ADDED);
        CfMapValueChanged mapValueRemoved = new CfMapValueChanged("removedKey",
                "",
                "removed",
                ChangeType.REMOVED);
        CfMapValueChanged mapValueChanged = new CfMapValueChanged("changedKey",
                "before",
                "changed",
                ChangeType.CHANGED);

        CfMapChange envVarsChange = new CfMapChange(applicationBean,
                "environmentVariables",
                Arrays.asList("path"),
                Arrays.asList(mapValueAdded, mapValueRemoved, mapValueChanged));

        cfChanges.add(envVarsChange);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).addEnvironmentVariable("testApp", "addedKey", "added");
        verify(appOperations, times(1)).addEnvironmentVariable("testApp", "changedKey", "changed");
        verify(appOperations, times(1)).removeEnvironmentVariable("testApp", "removedKey");
        verifyNoMoreInteractions(appOperations);
        StepVerifier.create(requests)
                .expectNext(voidMockAdded)
                .expectNext(voidMockRemoved)
                .expectNext(voidMockAdded)
                .expectComplete()
                .verify();
    }


    @Test
    void applyTest_OnlyServices() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);

        Void voidMockAdded = mock(Void.class);
        when(appOperations.bindToService(anyString(), anyString()))
                .thenReturn(Mono.just(voidMockAdded));
        Void voidMockRemoved = mock(Void.class);
        when(appOperations.unbindFromService(anyString(), anyString()))
                .thenReturn(Mono.just(voidMockRemoved));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfContainerValueChanged containerValueAdded = new CfContainerValueChanged("serviceAdded",
                ChangeType.ADDED);
        CfContainerValueChanged containerValueRemoved = new CfContainerValueChanged("serviceRemoved",
                ChangeType.REMOVED);

        CfContainerChange servicesChanges = new CfContainerChange(applicationBean,
                "services",
                Arrays.asList("path"),
                Arrays.asList(containerValueAdded, containerValueRemoved));

        cfChanges.add(servicesChanges);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).bindToService("testApp", "serviceAdded");
        verify(appOperations, times(1)).unbindFromService("testApp", "serviceRemoved");
        verifyNoMoreInteractions(appOperations);
        StepVerifier.create(requests)
                .expectNext(voidMockAdded)
                .expectNext(voidMockRemoved)
                .expectComplete()
                .verify();
    }

    @Test
    void applyTest_OnlyRoutes() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);

        Void voidMockAdded = mock(Void.class);
        when(appOperations.addRoute(anyString(), anyString()))
                .thenReturn(Mono.just(voidMockAdded));
        Void voidMockRemoved = mock(Void.class);
        when(appOperations.removeRoute(anyString(), anyString()))
                .thenReturn(Mono.just(voidMockRemoved));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfContainerValueChanged containerValueAdded = new CfContainerValueChanged("routeAdded",
                ChangeType.ADDED);
        CfContainerValueChanged containerValueRemoved = new CfContainerValueChanged("routeRemoved",
                ChangeType.REMOVED);

        CfContainerChange servicesChanges = new CfContainerChange(applicationBean,
                "routes",
                Arrays.asList("path"),
                Arrays.asList(containerValueAdded, containerValueRemoved));

        cfChanges.add(servicesChanges);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).addRoute("testApp", "routeAdded");
        verify(appOperations, times(1)).removeRoute("testApp", "routeRemoved");
        verifyNoMoreInteractions(appOperations);
        StepVerifier.create(requests)
                .expectNext(voidMockAdded)
                .expectNext(voidMockRemoved)
                .expectComplete()
                .verify();
    }


    @Test
    void applyTest_OnFieldThatRequiresRestartUpdatesTheApp() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        Void voidMock = mock(Void.class);
        when(appOperations.update(anyString(), any()))
                .thenReturn(Mono.just(voidMock));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfObjectValueChanged healthCheckTypeChange = new CfObjectValueChanged(applicationBean,
                "healthCheckType",
                Arrays.asList("path"),
                "PORT",
                "HTTP");

        cfChanges.add(healthCheckTypeChange);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).update("testApp", applicationBean);
        verifyNoMoreInteractions(appOperations);
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    @Test
    void applyTest_FieldThatRequiresRestartAndFieldThatDoesNotRequireRestart() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        Void voidUpdateMock = mock(Void.class);
        when(appOperations.update(anyString(), any()))
                .thenReturn(Mono.just(voidUpdateMock));
        Void voidAddEnvVarMock = mock(Void.class);
        when(appOperations.addEnvironmentVariable(anyString(), anyString(),anyString()))
                .thenReturn(Mono.just(voidAddEnvVarMock));
        Void voidRemoveEnvVarMock = mock(Void.class);
        when(appOperations.addEnvironmentVariable(anyString(), anyString(),anyString()))
                .thenReturn(Mono.just(voidRemoveEnvVarMock));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfObjectValueChanged healthCheckTypeChange = new CfObjectValueChanged(applicationBean,
                "healthCheckType",
                Arrays.asList("path"),
                "PORT",
                "HTTP");

        cfChanges.add(healthCheckTypeChange);

        CfMapValueChanged mapValueAdded = new CfMapValueChanged("addedKey",
                "",
                "added",
                ChangeType.ADDED);
        CfMapValueChanged mapValueRemoved = new CfMapValueChanged("removedKey",
                "",
                "removed",
                ChangeType.REMOVED);
        CfMapValueChanged mapValueChanged = new CfMapValueChanged("changedKey",
                "before",
                "changed",
                ChangeType.CHANGED);

        CfMapChange envVarsChange = new CfMapChange(applicationBean,
                "environmentVariables",
                Arrays.asList("path"),
                Arrays.asList(mapValueAdded, mapValueRemoved, mapValueChanged));

        cfChanges.add(envVarsChange);

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).update("testApp", applicationBean);
        verifyNoMoreInteractions(appOperations);
    }

    @Test
    void applyTest_EmptyChanges() {
        // given
        ApplicationsOperations appOperations = mock(ApplicationsOperations.class);
        Void voidMock = mock(Void.class);
        when(appOperations.update(anyString(), any()))
                .thenReturn(Mono.just(voidMock));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();

        ApplicationRequestsPlanner requestsPlanner = new ApplicationRequestsPlanner(appOperations);

        // when
        Flux<Void> requests = requestsPlanner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verifyNoMoreInteractions(appOperations);
        assertThat(requests.count().block(), is(0L));
    }



}
