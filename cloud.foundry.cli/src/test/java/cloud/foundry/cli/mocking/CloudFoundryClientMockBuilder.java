package cloud.foundry.cli.mocking;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;


/**
 * Builder class that creates a mock instance for the {@link CloudFoundryClient} class
 */
public class CloudFoundryClientMockBuilder {

    private ApplicationsV3 applicationsV3Mock;

    private CloudFoundryClientMockBuilder() { }

    /**
     * @return an instance of the builder
     */
    public static CloudFoundryClientMockBuilder get() {
        return new CloudFoundryClientMockBuilder();
    }

    /**
     * Set the applicationsv3 mock object
     * @param applicationsV3Mock mock of the {@link ApplicationsV3}
     * @return the builder instance
     */
    public CloudFoundryClientMockBuilder setApplicationsV3(ApplicationsV3 applicationsV3Mock) {
        checkNotNull(applicationsV3Mock);

        this.applicationsV3Mock = applicationsV3Mock;
        return this;
    }

    /**
     * @return a mock of the {@link CloudFoundryClient}
     */
    public CloudFoundryClient build() {
        CloudFoundryClient cloudFoundryClientMock = mock(CloudFoundryClient.class);

        when(cloudFoundryClientMock.applicationsV3())
                .thenReturn(this.applicationsV3Mock);

        return cloudFoundryClientMock;
    }

}
