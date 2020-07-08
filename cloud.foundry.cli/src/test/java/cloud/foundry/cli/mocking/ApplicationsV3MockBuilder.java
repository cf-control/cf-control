package cloud.foundry.cli.mocking;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleData;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationResponse;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationResponse;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Builder class that creates a mock instance for the {@link ApplicationsV3MockBuilder} class
 */
public class ApplicationsV3MockBuilder {

    private Map<String, Metadata> metadata;

    private ApplicationsV3MockBuilder() {

    }

    /**
     * @return an instance of the builder
     */
    public static ApplicationsV3MockBuilder get() {
        return new ApplicationsV3MockBuilder();
    }

    /**
     * Set the metadata of the apps that the cloud foundry instance should store
     * @param metadata map of the metadata of an app with the app id as key and a {@link Metadata} as value
     * @return the builder instance
     */
    public ApplicationsV3MockBuilder setMetadata(Map<String, Metadata> metadata) {
        checkNotNull(metadata);

        this.metadata = metadata;
        return this;
    }


    /**
     * @return a mock of the {@link DefaultCloudFoundryOperations}
     */
    public ApplicationsV3 build() {
        ApplicationsV3 applicationsV3Mock = mock(ApplicationsV3.class);

        mockUpdate(applicationsV3Mock);
        mockGet(applicationsV3Mock);

        return applicationsV3Mock;
    }

    private void mockGet(ApplicationsV3 applicationsV3Mock) {
        when(applicationsV3Mock.get(any(GetApplicationRequest.class)))
                .thenAnswer(invocation -> {
                    String appId = ((GetApplicationRequest) invocation.getArgument(0)).getApplicationId();

                    if (this.metadata.containsKey(appId)) {
                        GetApplicationResponse response = GetApplicationResponse
                                .builder()
                                .metadata(this.metadata.get(appId))
                                // random values without meaning
                                .createdAt("someday")
                                .lifecycle(Lifecycle
                                        .builder()
                                        .data(mock(LifecycleData.class))
                                        .type(LifecycleType.BUILDPACK)
                                        .build())
                                .name("somename")
                                .state(ApplicationState.STOPPED)
                                .id(appId)
                                .build();
                        return Mono.just(response);
                    }

                    return Mono.empty();
                });
    }

    private void mockUpdate(ApplicationsV3 applicationsV3Mock) {
        when(applicationsV3Mock.update(any(UpdateApplicationRequest.class)))
                .thenReturn(Mono.just(mock(UpdateApplicationResponse.class)));
    }
}
