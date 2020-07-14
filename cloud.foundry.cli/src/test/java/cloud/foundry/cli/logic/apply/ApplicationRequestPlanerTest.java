package cloud.foundry.cli.logic.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import cloud.foundry.cli.operations.ServicesOperations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.LinkedList;

class ApplicationRequestPlanerTest {

    @Test
    void applyTest_WithMultipleNewObject_AcceptMethodCallOnOnlyOne() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        when(appOperations.create(any(String.class), any(ApplicationBean.class), any(boolean.class)))
                .thenReturn(Mono.just(mock(Void.class)));

        String appName = "testApp";

        LinkedList<CfChange> cfChanges = new LinkedList<>();
        CfNewObject newObject = Mockito.mock(CfNewObject.class);
        ApplicationBean applicationBean = new ApplicationBean();
        when(newObject.getAffectedObject())
                .thenReturn(applicationBean);

        CfNewObject newObject2 = Mockito.mock(CfNewObject.class);

        cfChanges.add(newObject);
        cfChanges.add(newObject2);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).create("testApp", applicationBean, false);
    }

    @Test
    void applyTest_WithChangeObjectNotAppBeanOrAppManifestBean() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        CfNewObject newObject = new CfNewObject(serviceBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        assertThrows(ApplyException.class,
            () -> requestsPlaner.createApplyRequests(appName, cfChanges));
    }

    @Test
    void applyTest_WithNewChangeObject_AppCreated() throws CreationException {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean appBeanMock = mock(ApplicationBean.class);
        CfNewObject newObject = new CfNewObject(appBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);
        Void voidMock = mock(Void.class);
        Mono<Void> monoMock = Mono.just(voidMock);
        when(appOperations.create(appName, appBeanMock, false)).thenReturn(monoMock);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        verify(appOperations, times(1)).create(appName, appBeanMock, false);
        StepVerifier.create(requests)
            .expectNext(voidMock)
            .expectComplete()
            .verify();
    }

    @Test
    void applyTest_WithNewChangeObject_CreationException() throws CreationException {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean appBeanMock = mock(ApplicationBean.class);
        CfNewObject newObject = new CfNewObject(appBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);
        doThrow(new CreationException("Test")).when(appOperations).create(appName, appBeanMock, false);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        ApplyException applyException = assertThrows(ApplyException.class,
            () -> requestsPlaner.createApplyRequests(appName,  cfChanges));
        // then
        assertThat(applyException.getCause(), is(instanceOf(CreationException.class)));
    }

    @Test
    void applyTest_WithRemovedObject_AppRemoved() throws CreationException {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean appBeanMock = mock(ApplicationBean.class);
        CfRemovedObject removedObject = new CfRemovedObject(appBeanMock, "propertyName", Arrays.asList("path"));
        cfChanges.add(removedObject);

        Void voidMock = mock(Void.class);
        Mono<Void> monoMock = Mono.just(voidMock);
        when(appOperations.remove(appName)).thenReturn(monoMock);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

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
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        CfRemovedObject removedObject = new CfRemovedObject(serviceBeanMock, "propertyName", Arrays.asList("path"));
        cfChanges.add(removedObject);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        assertThrows(ApplyException.class,
            () -> requestsPlaner.createApplyRequests(appName, cfChanges));
    }


    @Test
    void applyTest_OnlyScalableField() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
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

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(1)).scale("testApp", null, null, 4);
        verify(appOperations, times(0)).addEnvironmentVariable(anyString(), anyString(), anyString());
        verify(appOperations, times(0)).removeEnvironmentVariable(anyString(), anyString());
        verify(appOperations, times(0)).bindToService(anyString(), anyString());
        verify(appOperations, times(0)).unbindFromService(anyString(), anyString());

    }

    @Test
    void applyTest_OnlyEnvironmentVariables() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        Void voidMockAdded = mock(Void.class);
        when(appOperations.addEnvironmentVariable(any(), any(), any()))
                .thenReturn(Mono.just(voidMockAdded));
        Void voidMockRemoved = mock(Void.class);
        when(appOperations.removeEnvironmentVariable(any(), any()))
                .thenReturn(Mono.just(voidMockRemoved));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfMapValueChanged mapValueAdded = new CfMapValueChanged("addedKey", "", "added", ChangeType.ADDED);
        CfMapValueChanged mapValueRemoved = new CfMapValueChanged("removedKey", "", "removed", ChangeType.REMOVED);
        CfMapValueChanged mapValueChanged = new CfMapValueChanged("changedKey", "before", "changed", ChangeType.CHANGED);

        CfMapChange envVarsChange = new CfMapChange(applicationBean,
                "environmentVariables",
                Arrays.asList("path"),
                Arrays.asList(mapValueAdded, mapValueRemoved, mapValueChanged));

        cfChanges.add(envVarsChange);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(0)).create(anyString(), any(), anyBoolean());
        verify(appOperations, times(0)).update(anyString(), any(), anyBoolean());
        verify(appOperations, times(0)).remove(anyString());
        verify(appOperations, times(0)).scale(anyString(), anyInt(), anyInt(), anyInt());
        verify(appOperations, times(1)).addEnvironmentVariable("testApp", "addedKey", "added");
        verify(appOperations, times(1)).addEnvironmentVariable("testApp", "changedKey", "changed");
        verify(appOperations, times(1)).removeEnvironmentVariable("testApp", "removedKey");
        verify(appOperations, times(0)).bindToService(anyString(), anyString());
        verify(appOperations, times(0)).unbindFromService(anyString(), anyString());
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
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);

        Void voidMockAdded = mock(Void.class);
        when(appOperations.bindToService(anyString(), anyString()))
                .thenReturn(Mono.just(voidMockAdded));
        Void voidMockRemoved = mock(Void.class);
        when(appOperations.unbindFromService(anyString(), anyString()))
                .thenReturn(Mono.just(voidMockRemoved));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ApplicationBean applicationBean = new ApplicationBean();

        CfContainerValueChanged containerValueAdded = new CfContainerValueChanged("serviceAdded", ChangeType.ADDED);
        CfContainerValueChanged containerValueRemoved = new CfContainerValueChanged("serviceRemoved", ChangeType.REMOVED);

        CfContainerChange servicesChanges = new CfContainerChange(applicationBean,
                "services",
                Arrays.asList("path"),
                Arrays.asList(containerValueAdded, containerValueRemoved));

        cfChanges.add(servicesChanges);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(0)).create(anyString(), any(), anyBoolean());
        verify(appOperations, times(0)).update(anyString(), any(), anyBoolean());
        verify(appOperations, times(0)).remove(anyString());
        verify(appOperations, times(0)).scale(anyString(), anyInt(), anyInt(), anyInt());
        verify(appOperations, times(0)).addEnvironmentVariable(anyString(), anyString(), anyString());
        verify(appOperations, times(0)).removeEnvironmentVariable(anyString(), anyString());
        verify(appOperations, times(1)).bindToService("testApp", "serviceAdded");
        verify(appOperations, times(1)).unbindFromService("testApp", "serviceRemoved");
        StepVerifier.create(requests)
                .expectNext(voidMockAdded)
                .expectNext(voidMockRemoved)
                .expectComplete()
                .verify();
    }


    @Test
    void applyTest_OnFieldThatRequiresRestartUpdatesTheApp() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        Void voidMock = mock(Void.class);
        when(appOperations.update(anyString(), any(), anyBoolean()))
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

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(0)).create(anyString(), any(), anyBoolean());
        verify(appOperations, times(1)).update("testApp", applicationBean, true);
        verify(appOperations, times(0)).remove(anyString());
        verify(appOperations, times(0)).scale(anyString(), anyInt(), anyInt(), anyInt());
        verify(appOperations, times(0)).addEnvironmentVariable(anyString(), anyString(), anyString());
        verify(appOperations, times(0)).bindToService(anyString(), anyString());
        verify(appOperations, times(0)).unbindFromService(anyString(), anyString());
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    @Test
    void applyTest_FieldThatRequiresRestartAndFieldThatDoesNotRequireRestart() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        Void voidUpdateMock = mock(Void.class);
        when(appOperations.update(anyString(), any(), anyBoolean()))
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

        CfMapValueChanged mapValueAdded = new CfMapValueChanged("addedKey", "", "added", ChangeType.ADDED);
        CfMapValueChanged mapValueRemoved = new CfMapValueChanged("removedKey", "", "removed", ChangeType.REMOVED);
        CfMapValueChanged mapValueChanged = new CfMapValueChanged("changedKey", "before", "changed", ChangeType.CHANGED);

        CfMapChange envVarsChange = new CfMapChange(applicationBean,
                "environmentVariables",
                Arrays.asList("path"),
                Arrays.asList(mapValueAdded, mapValueRemoved, mapValueChanged));

        cfChanges.add(envVarsChange);

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(0)).create(anyString(), any(), anyBoolean());
        verify(appOperations, times(1)).update("testApp", applicationBean, true);
        verify(appOperations, times(0)).remove(anyString());
        verify(appOperations, times(0)).scale(anyString(), anyInt(), anyInt(), anyInt());
        verify(appOperations, times(0)).addEnvironmentVariable("testApp", "addedKey", "added");
        verify(appOperations, times(0)).addEnvironmentVariable("testApp", "changedKey", "changed");
        verify(appOperations, times(0)).removeEnvironmentVariable("testApp", "removedKey");
        verify(appOperations, times(0)).bindToService(anyString(), anyString());
        verify(appOperations, times(0)).unbindFromService(anyString(), anyString());
    }

    @Test
    void applyTest_EmptyChanges() {
        // given
        ApplicationsOperations appOperations = Mockito.mock(ApplicationsOperations.class);
        Void voidMock = mock(Void.class);
        when(appOperations.update(anyString(), any(), anyBoolean()))
                .thenReturn(Mono.just(voidMock));

        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();

        ApplicationRequestsPlaner requestsPlaner = new ApplicationRequestsPlaner(appOperations);

        // when
        Flux<Void> requests = requestsPlaner.createApplyRequests(appName, cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(appOperations, times(0)).create(anyString(), any(), anyBoolean());
        verify(appOperations, times(0)).update(anyString(), any(), anyBoolean());
        verify(appOperations, times(0)).remove(anyString());
        verify(appOperations, times(0)).scale(anyString(), anyInt(), anyInt(), anyInt());
        verify(appOperations, times(0)).addEnvironmentVariable(anyString(), anyString(), anyString());
        verify(appOperations, times(0)).bindToService(anyString(), anyString());
        verify(appOperations, times(0)).unbindFromService(anyString(), anyString());
        assertThat(requests.count().block(), is(0L));
    }



}
