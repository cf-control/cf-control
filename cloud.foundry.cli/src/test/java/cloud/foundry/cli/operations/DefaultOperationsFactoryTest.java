package cloud.foundry.cli.operations;

import cloud.foundry.cli.operations.client.DefaultClientOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DefaultOperationsFactoryTest {

    @BeforeAll
    static void setup() {
        DefaultCloudFoundryOperations cfMock = mock(DefaultCloudFoundryOperations.class);
        OperationsFactory.setInstance(new DefaultOperationsFactory(cfMock));
    }

    @Test
    public void testConstructorThrowsExceptionWhenParameterNull() {
        assertThrows(NullPointerException.class, () -> new DefaultOperationsFactory(null));
    }

    @Test
    public void testCreateApplicationsOperations() {
        ApplicationsOperations applicationsOperations = DefaultOperationsFactory.getInstance().createApplicationsOperations();

        assertThat(applicationsOperations, notNullValue());
    }

    @Test
    public void testCreateServiceOperations() {
        ServicesOperations servicesOperations = DefaultOperationsFactory.getInstance().createServiceOperations();

        assertThat(servicesOperations, notNullValue());
    }

    @Test
    public void testCreateSpaceDevelopersOperations() {
        SpaceDevelopersOperations spaceDevelopersOperations = DefaultOperationsFactory.getInstance().createSpaceDevelopersOperations();

        assertThat(spaceDevelopersOperations, notNullValue());
    }

    @Test
    public void testCreateClientOperations() {
        ClientOperations clientOperations = DefaultOperationsFactory.getInstance().createClientOperations();

        assertThat(clientOperations, notNullValue());
    }

    @Test
    public void testCreateSpaceOperations() {
        SpaceOperations spaceOperations = DefaultOperationsFactory.getInstance().createSpaceOperations();

        assertThat(spaceOperations, notNullValue());
    }
}
