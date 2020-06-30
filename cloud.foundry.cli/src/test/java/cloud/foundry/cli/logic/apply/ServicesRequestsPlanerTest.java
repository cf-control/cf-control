package cloud.foundry.cli.logic.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ServicesOperations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class ServicesRequestsPlanerTest {

    @Test
    public void testCreateWithNewObjectSucceeds() {
        // given
        ServiceBean newServiceMock = mock(ServiceBean.class);
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange newChange = new CfNewObject(newServiceMock, "someservice", Collections.singletonList("root"));
        Mono<Void> monoMock = mock(Mono.class);
        Mockito.when(servicesOperations.create("someservice", newServiceMock)).thenReturn(monoMock);
        List<CfChange> cfChanges = new LinkedList<>();
        cfChanges.add(newChange);
        // when
        Flux<Void> requests = ServiceRequestsPlaner.create(servicesOperations,
                "someservice",
                cfChanges);

        //then
        assertThat(requests, notNullValue());
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
                () -> ServiceRequestsPlaner.create(servicesOperations, "someservice", cfChanges));
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
                () -> ServiceRequestsPlaner.create(servicesOperations, "someservice", cfChanges));
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
                () -> ServiceRequestsPlaner.create(servicesOperations, "someservice", Arrays.asList(remove1, remove2)));
    }

    @Test
    public void testCreateWithRemovedObjectThrowsExceptionWhenAffectedObjectIsNotOfTypeServiceBean() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);

        CfChange remove1 = new CfRemovedObject(new ConfigBean(), "someservice", Collections.singletonList("root"));

        // when and then
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlaner.create(servicesOperations, "someservice", Arrays.asList(remove1)));
    }

    @Test
    public void testCreateWithRemovedObjectThrowsApplyExceptionWhenUnderlyingExceptionGetsThrown() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        when(servicesOperations.remove("someservice")).thenThrow(new UpdateException(null));

        CfChange remove1 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));

        // when and then
        assertThrows(ApplyException.class,
                () -> ServiceRequestsPlaner.create(servicesOperations, "someservice", Arrays.asList(remove1)));
    }

    @Test
    public void testCreateWithRemovedObjectSucceeds() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        CfChange remove1 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));
        Mockito.when(servicesOperations.remove("someservice")).thenReturn(Mono.empty());

        // when
        Flux<Void> requests = ServiceRequestsPlaner.create(servicesOperations,
                "someservice",
                Arrays.asList(remove1));

        //then
        assertThat(requests, notNullValue());
    }
}
