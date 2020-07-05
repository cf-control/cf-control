package cloud.foundry.cli.mocking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationResponse;
import reactor.core.publisher.Mono;


/**
 * Builder class that creates a mock instance for the {@link CloudFoundryClient} class
 */
public class CloudFoundryClientMockBuilder {

    private CloudFoundryClientMockBuilder() { }

    /**
     * @return an instance of the builder
     */
    public static CloudFoundryClientMockBuilder get() {
        return new CloudFoundryClientMockBuilder();
    }

    /**
     * @return a mock of the {@link CloudFoundryClient}
     */
    public CloudFoundryClient build() {
        CloudFoundryClient cloudFoundryClientMock = mock(CloudFoundryClient.class);

        ApplicationsV3 applicationsV3Mock = mock(ApplicationsV3.class);
        when(applicationsV3Mock.update(any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.just(mock(UpdateApplicationResponse.class)));

        when(cloudFoundryClientMock.applicationsV3())
                .thenReturn(applicationsV3Mock);

        return cloudFoundryClientMock;
    }

}
