package cloud.foundry.cli.logic.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ServicesOperations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ServicesRequestsPlannerTest {

    @Test
    public void testCreateWithNewObjectSucceeds() {
        // given
        ServiceBean newServiceMock = mock(ServiceBean.class);
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange newChange = new CfNewObject(newServiceMock, "someservice", Collections.singletonList("root"));
        Void voidMock = mock(Void.class);
        Mockito.when(servicesOperations.create("someservice", newServiceMock)).thenReturn(Mono.just(voidMock));
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);
        // when
        Flux<Void> requests = ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                "someservice",
                cfChanges);

        // then
        assertThat(requests, notNullValue());
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    @Test
    public void testCreateWithNewObjectNullpointerException() {
        // given
        ServiceBean newServiceBean = new ServiceBean();
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange newChange = new CfNewObject(newServiceBean, "someservice",
                Collections.singletonList("root"));
        Mockito.when(servicesOperations.create("someservice", newServiceBean))
                .thenThrow(new NullPointerException());
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);
        // when
        assertThrows(ApplyException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations, "someservice", cfChanges));
    }

    @Test
    public void testCreateWithNewObjectNotServiceBean() {
        // given
        ApplicationBean newApplicationBean = new ApplicationBean();
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange newChange = new CfNewObject(newApplicationBean, "someservice",
                Collections.singletonList("root"));
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);
        // when
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations, "someservice", cfChanges));
    }

    @Test
    public void testCreateOnNullArgumentsThrowsNullPointerException() {
        // when and then
        assertThrows(NullPointerException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(null,
                        "someservice",
                        Collections.emptyList()));
        assertThrows(NullPointerException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(mock(ServicesOperations.class),
                        null,
                        Collections.emptyList()));
        assertThrows(NullPointerException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(mock(ServicesOperations.class),
                        "someservice",
                        null));
    }

    @Test
    public void testCreateWithRemovedObjectThrowsExceptionWhenThereAreAlreadyOtherRequests() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        when(servicesOperations.remove("someservice")).thenReturn(Mono.empty());

        CfChange remove1 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));
        CfChange remove2 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));

        // when and then
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                        "someservice",
                        Arrays.asList(remove1, remove2)));
    }

    @Test
    public void testCreateWithRemovedObjectThrowsExceptionWhenAffectedObjectIsNotOfTypeServiceBean() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);

        CfChange remove1 = new CfRemovedObject(new ConfigBean(), "someservice", Collections.singletonList("root"));

        // when and then
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                        "someservice",
                        Arrays.asList(remove1)));
    }

    @Test
    public void testCreateWithRemovedObjectThrowsApplyExceptionWhenUnderlyingExceptionGetsThrown() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        when(servicesOperations.remove("someservice")).thenThrow(new UpdateException(null));

        CfChange remove1 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));

        // when and then
        assertThrows(ApplyException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                        "someservice",
                        Arrays.asList(remove1)));
    }

    @Test
    public void testCreateWithRemovedObjectSucceeds() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange remove1 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));
        Void voidMock = mock(Void.class);
        Mockito.when(servicesOperations.remove("someservice")).thenReturn(Mono.just(voidMock));

        // when
        Flux<Void> requests = ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                "someservice",
                Arrays.asList(remove1));

        // then
        assertThat(requests, notNullValue());
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    @Test
    public void testCreateWithRemovedCfMapChangeThrowsApplyException() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange mapchange = new CfMapChange(new ServiceBean(),
                "someservice",
                Collections.singletonList("root"),
                Arrays.asList(new CfMapValueChanged("key", "valueBefore", "valueAfter", ChangeType.CHANGED)));

        // when
        ApplyException exception = assertThrows(ApplyException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                        "someservice",
                        Arrays.asList(mapchange)));

        assertThat(exception.getMessage(), is("Change type is not supported."));
    }

    @Test
    public void testCreateWithObjectValueChangedSucceeds() {
        // given
        String serviceName = "serviceName";
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange objectChanged = new CfObjectValueChanged(serviceBeanMock, serviceName,
                Collections.singletonList("root"),
                "valueBefore", "valueAfter");
        Void voidMock = mock(Void.class);
        Mockito.when(servicesOperations.update(serviceName, serviceBeanMock)).thenReturn(Mono.just(voidMock));
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(objectChanged);

        // when
        Flux<Void> requests = ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                serviceName,
                cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(servicesOperations, times(1)).update(serviceName, serviceBeanMock);
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    @Test
    public void testCreateWithValueChangedNullpointerException() {
        // given
        String serviceName = "serviceName";
        ServiceBean serviceBean = new ServiceBean();
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange newChange = new CfNewObject(serviceBean, serviceName,
                Collections.singletonList("root"));
        Mockito.when(servicesOperations.update(serviceName, serviceBean))
                .thenThrow(new NullPointerException());
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);

        // then - when
        assertThrows(ApplyException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations, serviceName, cfChanges));
    }

    @Test
    public void testCreateWithObjectValueChangedNotServiceBean() {
        // given
        String serviceName = "serviceName";
        ApplicationBean newApplicationBean = new ApplicationBean();
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange newChange = new CfObjectValueChanged(newApplicationBean, serviceName,
                Collections.singletonList("root"),
                "valueBefore", "valueAfter");
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);

        // then - when
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations, serviceName, cfChanges));
    }

    @Test
    public void testCreateWithCfContainerChangeSucceeds() {
        // given
        String serviceName = "serviceName";
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        ServicesOperations servicesOperations = mock(ServicesOperations.class);

        List<CfContainerValueChanged> changedValues = createCfContainerChange();
        CfChange newChange = new CfContainerChange(serviceBeanMock, serviceName,
                Collections.singletonList("root"),
                changedValues);

        Void voidMock = mock(Void.class);
        Mockito.when(servicesOperations.update(serviceName, serviceBeanMock)).thenReturn(Mono.just(voidMock));
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);

        // when
        Flux<Void> requests = ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                serviceName,
                cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(servicesOperations, times(1)).update(serviceName, serviceBeanMock);
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    @Test
    public void testCreateWithCfContainerChangeNotServiceBean() {
        // given
        String serviceName = "serviceName";
        ApplicationBean newApplicationBean = new ApplicationBean();
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        List<CfContainerValueChanged> changedValues = createCfContainerChange();
        CfChange newChange = new CfContainerChange(newApplicationBean, serviceName,
                Collections.singletonList("root"),
                changedValues);

        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);

        // then - when
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlanner.createApplyRequests(servicesOperations, serviceName, cfChanges));
    }

    @Test
    public void testCreateWithUpdateServiceSucceeds() {
        // given
        String serviceName = "serviceName";
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        ServicesOperations servicesOperations = mock(ServicesOperations.class);

        List<CfContainerValueChanged> changedValues = createCfContainerChange();
        CfChange containerChanged = new CfContainerChange(serviceBeanMock, serviceName,
                Collections.singletonList("root"),
                changedValues);

        CfChange objectChanged = new CfObjectValueChanged(serviceBeanMock, serviceName,
                Collections.singletonList("root"),
                "valueBefore", "valueAfter");

        Void voidMock = mock(Void.class);
        Mockito.when(servicesOperations.update(serviceName, serviceBeanMock)).thenReturn(Mono.just(voidMock));
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(containerChanged);
        cfChanges.add(objectChanged);

        // when
        Flux<Void> requests = ServiceRequestsPlanner.createApplyRequests(servicesOperations,
                serviceName,
                cfChanges);

        // then
        assertThat(requests, notNullValue());
        verify(servicesOperations, times(1)).update(serviceName, serviceBeanMock);
        StepVerifier.create(requests)
                .expectNext(voidMock)
                .expectComplete()
                .verify();
    }

    private List<CfContainerValueChanged> createCfContainerChange() {
        CfContainerValueChanged containerValueAdded = new CfContainerValueChanged("tags1", ChangeType.ADDED);
        CfContainerValueChanged containerValueRemoved = new CfContainerValueChanged("tags2", ChangeType.REMOVED);

        List<CfContainerValueChanged> changedValues = new LinkedList<>();
        changedValues.add(containerValueAdded);
        changedValues.add(containerValueRemoved);

        return changedValues;
    }
}
