package cloud.foundry.cli.logic.apply;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Arrays;
import java.util.LinkedList;

class ApplicationApplierTest {

    @Test
    void applyTest_With1ChangeObject_AcceptMethodCalledOnChangeObject() {
        //given
        DefaultCloudFoundryOperations cloudFoundryOperations = Mockito.mock(DefaultCloudFoundryOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        CfNewObject newObject = Mockito.mock(CfNewObject.class);
        CfNewObject newObject2 = Mockito.mock(CfNewObject.class);
        cfChanges.add(newObject);
        cfChanges.add(newObject2);
        //when
        ApplicationApplier.apply(cloudFoundryOperations, appName, cfChanges);
        //then
        verify(newObject, times(1)).accept(any());
        verify(newObject2, times(1)).accept(any());
    }

    @Test
    void applyTest_WithChangeObjectNotAppBeanOrAppManifestBean() {
        //given
        DefaultCloudFoundryOperations cloudFoundryOperations = Mockito.mock(DefaultCloudFoundryOperations.class);
        String appName = "testApp";
        LinkedList<CfChange> cfChanges = new LinkedList<>();
        ServiceBean serviceBeanMock = mock(ServiceBean.class);
        CfNewObject newObject = new CfNewObject(serviceBeanMock, "", Arrays.asList("path"));
        cfChanges.add(newObject);
        //when
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationApplier.apply(cloudFoundryOperations, appName, cfChanges));
    }

}