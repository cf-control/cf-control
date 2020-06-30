package cloud.foundry.cli.logic.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
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


public class ServicesRequestsPlanerTest {

    @Test
    public void testCreateWithRemovedObjectThrowsExceptionWhenThereAreAlreadyOtherRequests() {
        // given
        ServicesOperations servicesOperations = mock(ServicesOperations.class);
        when(servicesOperations.remove("someservice")).thenReturn(Mono.empty());

        CfChange remove1 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));
        CfChange remove2 = new CfRemovedObject(new ServiceBean(), "someservice", Collections.singletonList("root"));

        // when and then
        assertThrows(IllegalArgumentException.class,
                () -> ServiceRequestsPlaner.createApplyRequests(servicesOperations,
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
                () -> ServiceRequestsPlaner.createApplyRequests(servicesOperations,
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
                () -> ServiceRequestsPlaner.createApplyRequests(servicesOperations,
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
        Flux<Void> requests = ServiceRequestsPlaner.createApplyRequests(servicesOperations,
                "someservice",
                Arrays.asList(remove1));

        //then
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
                () -> ServiceRequestsPlaner.createApplyRequests(servicesOperations,
                        "someservice",
                        Arrays.asList(mapchange)));

        assertThat(exception.getMessage(), is("Change type is not supported."));
    }
}
