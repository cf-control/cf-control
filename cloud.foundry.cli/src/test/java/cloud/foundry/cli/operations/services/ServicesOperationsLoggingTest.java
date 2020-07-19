package cloud.foundry.cli.operations.services;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.operations.ServicesOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class ServicesOperationsLoggingTest {

    private ServicesOperations servicesOpMock;
    private ServicesOperationsLogging servicesOpLogging;

    @BeforeEach
    void setup() {
        servicesOpMock = mock(ServicesOperations.class);
        servicesOpLogging = new ServicesOperationsLogging(servicesOpMock);
    }

    @Test
    public void testGetAll() {
        // given
        when(servicesOpMock.getAll()).thenReturn(Mono.empty());

        // when
        servicesOpLogging.getAll();

        // then
        verify(servicesOpMock, times(1)).getAll();
    }

    @Test
    public void testCreate() {
        // given
        when(servicesOpMock.create(anyString(), any(ServiceBean.class))).thenReturn(Mono.empty());

        // when
        servicesOpLogging.create("service", new ServiceBean());

        // then
        verify(servicesOpMock, times(1)).create(eq("service"), any(ServiceBean.class));
    }

    @Test
    public void testRename() {
        // given
        when(servicesOpMock.rename(anyString(), anyString())).thenReturn(Mono.empty());

        // when
        servicesOpLogging.rename("service", "oldName");

        // then
        verify(servicesOpMock, times(1)).rename("service", "oldName");
    }

    @Test
    public void testUpdate() {
        // given
        when(servicesOpMock.update(anyString(), any(ServiceBean.class))).thenReturn(Mono.empty());

        // when
        servicesOpLogging.update("service", new ServiceBean());

        // then
        verify(servicesOpMock, times(1)).update(eq("service"), any(ServiceBean.class));
    }

    @Test
    public void testRemove() {
        // given
        when(servicesOpMock.remove(anyString())).thenReturn(Mono.empty());

        // when
        servicesOpLogging.remove("service");

        // then
        verify(servicesOpMock, times(1)).remove("service");
    }

    @Test
    public void testDeleteKeys() {
        // given
        when(servicesOpMock.deleteKeys(anyString())).thenReturn(Flux.empty());

        // when
        servicesOpLogging.deleteKeys("service");

        // then
        verify(servicesOpMock, times(1)).deleteKeys("service");
    }

    @Test
    public void testUnbindApps() {
        // given
        when(servicesOpMock.unbindApps(anyString())).thenReturn(Flux.empty());

        // when
        servicesOpLogging.unbindApps("service");

        // then
        verify(servicesOpMock, times(1)).unbindApps("service");
    }

    @Test
    public void testUnbindUnbindRoutes() {
        // given
        when(servicesOpMock.unbindRoutes(anyString())).thenReturn(Flux.empty());

        // when
        servicesOpLogging.unbindRoutes("service");

        // then
        verify(servicesOpMock, times(1)).unbindRoutes("service");
    }
}
