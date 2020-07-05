package cloud.foundry.cli.mocking;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Applications;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

/**
 * Builder class that creates a mock instance for the {@link DefaultCloudFoundryOperations} class
 */
public class DefaultCloudFoundryOperationsMockBuilder {

    private Map<String, ApplicationManifest> apps;
    private Throwable pushAppManifestException;

    private DefaultCloudFoundryOperationsMockBuilder() {
        this.apps = Collections.emptyMap();
    }

    /**
     * @return an instance of the builder
     */
    public static DefaultCloudFoundryOperationsMockBuilder get() {
        return new DefaultCloudFoundryOperationsMockBuilder();
    }

    /**
     * Set the applications that the cloud foundry instance should store
     * @param apps map of the app id as key and an {@link ApplicationManifest} as value
     * @return the builder instance
     */
    public DefaultCloudFoundryOperationsMockBuilder setApplications(Map<String, ApplicationManifest> apps) {
        checkNotNull(apps);

        this.apps = apps;
        return this;
    }

    /**
     * Set an error that gets thrown when pushApplicationManifest is invoked
     * @param throwable the exception that should be thrown
     * @return the builder instance
     */
    public DefaultCloudFoundryOperationsMockBuilder setPushApplicationManifestError(Throwable throwable) {
        this.pushAppManifestException = throwable;
        return this;
    }

    /**
     * @return a mock of the {@link DefaultCloudFoundryOperations}
     */
    public DefaultCloudFoundryOperations build() {
        DefaultCloudFoundryOperations cfOperationsMock = Mockito.mock(DefaultCloudFoundryOperations.class);

        mockApplications(cfOperationsMock);
        mockCloudFoundryClient(cfOperationsMock);

        return cfOperationsMock;
    }

    private void mockCloudFoundryClient(DefaultCloudFoundryOperations cfOperationsMock) {
        CloudFoundryClient cloudFoundryClientMock = CloudFoundryClientMockBuilder.get().build();
        when(cfOperationsMock.getCloudFoundryClient())
                .thenReturn(cloudFoundryClientMock);
    }

    private void mockApplications(DefaultCloudFoundryOperations cfOperationsMock) {
        Applications applicationsMock = ApplicationsMockBuilder.get().setApps(apps).setPushApplicationManifestError(pushAppManifestException).build();
        when(cfOperationsMock.applications())
                .thenReturn(applicationsMock);
    }

}
