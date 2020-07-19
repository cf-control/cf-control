package cloud.foundry.cli.operations.applications;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Test for {@link ApplicationsOperationsLogging}
 */
public class ApplicationsOperationsLoggingTest {

    private ApplicationsOperations appOpMock;
    private ApplicationsOperationsLogging apOpLogging;

    @BeforeEach
    void setup() {
        appOpMock = mock(ApplicationsOperations.class);
        apOpLogging = new ApplicationsOperationsLogging(appOpMock);
    }

    @Test
    public void testConstructorThrowsExceptionWhenParameterNull() {
        assertThrows(NullPointerException.class, () -> new ApplicationsOperationsLogging(null));
    }

    @Test
    public void testGetAll() {
        when(appOpMock.getAll()).thenReturn(Mono.empty());

        apOpLogging.getAll();

        verify(appOpMock, times(1)).getAll();
    }

    @Test
    public void testRemove() {
        when(appOpMock.remove(anyString())).thenReturn(Mono.empty());

        apOpLogging.remove("app");

        verify(appOpMock, times(1)).remove("app");
    }

   @Test
    public void testUpdate() {
        when(appOpMock.update(anyString(), any(), anyBoolean())).thenReturn(Mono.empty());

        ApplicationBean applicationBean = new ApplicationBean();
        apOpLogging.update("app", applicationBean, true);

        verify(appOpMock, times(1)).update("app", applicationBean, true);
    }

    @Test
    public void testCreate() {
        when(appOpMock.create(anyString(), any(), anyBoolean())).thenReturn(Mono.empty());

        ApplicationBean applicationBean = new ApplicationBean();
        apOpLogging.create("app", applicationBean, true);

        verify(appOpMock, times(1)).create("app", applicationBean, true);
    }

    @Test
    public void testRename() {
        when(appOpMock.rename(anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.rename("oldName", "newName");

        verify(appOpMock, times(1)).rename("oldName", "newName");
    }

    @Test
    public void testScale() {
        when(appOpMock.scale(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(Mono.empty());

        apOpLogging.scale("app", 1024, 1024, 4);

        verify(appOpMock, times(1)).scale("app", 1024, 1024, 4);
    }

    @Test
    public void testAddEnvironmentVariable() {
        when(appOpMock.addEnvironmentVariable(anyString(), anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.addEnvironmentVariable("app", "key", "value" );

        verify(appOpMock, times(1)).addEnvironmentVariable("app", "key", "value" );
    }

    @Test
    public void testRemoveEnvironmentVariable() {
        when(appOpMock.removeEnvironmentVariable(anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.removeEnvironmentVariable("app", "key");

        verify(appOpMock, times(1)).removeEnvironmentVariable("app", "key");
    }

    @Test
    public void testSetHealthCheck() {
        when(appOpMock.setHealthCheck(anyString(), any(ApplicationHealthCheck.class))).thenReturn(Mono.empty());

        apOpLogging.setHealthCheck("app", ApplicationHealthCheck.HTTP);

        verify(appOpMock, times(1)).setHealthCheck("app", ApplicationHealthCheck.HTTP);
    }

    @Test
    public void testBindToService() {
        when(appOpMock.bindToService(anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.bindToService("app", "service");

        verify(appOpMock, times(1)).bindToService("app", "service");
    }

    @Test
    public void testUnbindFromService() {
        when(appOpMock.unbindFromService(anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.unbindFromService("app", "service");

        verify(appOpMock, times(1)).unbindFromService("app", "service");
    }

    @Test
    public void testAddRoute() {
        when(appOpMock.addRoute(anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.addRoute("app", "route");

        verify(appOpMock, times(1)).addRoute("app", "route");
    }

    @Test
    public void testRemoveRoute() {
        when(appOpMock.removeRoute(anyString(), anyString())).thenReturn(Mono.empty());

        apOpLogging.removeRoute("app", "route");

        verify(appOpMock, times(1)).removeRoute("app", "route");
    }

}