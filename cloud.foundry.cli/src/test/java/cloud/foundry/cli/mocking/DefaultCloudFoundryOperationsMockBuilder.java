package cloud.foundry.cli.mocking;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * Builder class that creates a mock instance for the {@link DefaultCloudFoundryOperations} class
 */
public class DefaultCloudFoundryOperationsMockBuilder {

    private Applications applications;
    private CloudFoundryClient cloudFoundryClient;
    private String spaceId;

    private DefaultCloudFoundryOperationsMockBuilder() {

    }

    /**
     * @return an instance of the builder
     */
    public static DefaultCloudFoundryOperationsMockBuilder get() {
        return new DefaultCloudFoundryOperationsMockBuilder();
    }

    /**
     * Set the applications mock object
     * @param applicationsMock mock of the {@link Applications}
     * @return the builder instance
     */
    public DefaultCloudFoundryOperationsMockBuilder setApplications(Applications applicationsMock) {
        checkNotNull(applicationsMock);

        this.applications = applicationsMock;
        return this;
    }

    /**
     * Set the cloud foundry client mock object
     * @param cloudFoundryClientMock mock of the {@link CloudFoundryClient}
     * @return the builder instance
     */
    public DefaultCloudFoundryOperationsMockBuilder setCloudFoundryClient(CloudFoundryClient cloudFoundryClientMock) {
        checkNotNull(cloudFoundryClientMock);

        this.cloudFoundryClient = cloudFoundryClientMock;
        return this;
    }

    /**
     * Set the spaceId of the mock object
     * @param spaceId mock of the {@link CloudFoundryClient}
     * @return the builder instance
     */
    public DefaultCloudFoundryOperationsMockBuilder setSpaceId(String spaceId) {
        checkNotNull(spaceId);

        this.spaceId = spaceId;
        return this;
    }

    /**
     * @return a mock of the {@link DefaultCloudFoundryOperations}
     */
    public DefaultCloudFoundryOperations build() {
        DefaultCloudFoundryOperations cfOperationsMock = Mockito.mock(DefaultCloudFoundryOperations.class);

        mockApplications(cfOperationsMock);
        mockCloudFoundryClient(cfOperationsMock);
        mockGetSpaceId(cfOperationsMock);

        return cfOperationsMock;
    }

    private void mockGetSpaceId(DefaultCloudFoundryOperations cfOperationsMock) {
        when(cfOperationsMock.getSpaceId())
                .thenReturn(spaceId != null ? Mono.just(spaceId) : Mono.empty());
    }

    private void mockCloudFoundryClient(DefaultCloudFoundryOperations cfOperationsMock) {
        when(cfOperationsMock.getCloudFoundryClient())
                .thenReturn(this.cloudFoundryClient);
    }

    private void mockApplications(DefaultCloudFoundryOperations cfOperationsMock) {
        when(cfOperationsMock.applications())
                .thenReturn(this.applications);
    }

}
