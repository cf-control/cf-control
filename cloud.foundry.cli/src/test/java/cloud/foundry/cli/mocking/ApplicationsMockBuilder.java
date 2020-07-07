package cloud.foundry.cli.mocking;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder class that creates a mock instance for the {@link Applications} class
 */
public class ApplicationsMockBuilder {

    private Map<String, ApplicationManifest> apps;
    private Throwable pushAppManifestException;

    private ApplicationsMockBuilder() {
        this.apps = Collections.emptyMap();
    }

    /**
     * @return an instance of the builder
     */
    public static ApplicationsMockBuilder get() {
        return new ApplicationsMockBuilder();
    }

    /**
     * Set the applications that the cloud foundry instance should store
     * @param apps map of the app id as key and an {@link ApplicationManifest} as value
     * @return the builder instance
     */
    public ApplicationsMockBuilder setApps(Map<String, ApplicationManifest> apps) {
        checkNotNull(apps);

        this.apps = apps;
        return this;
    }

    /**
     * Set an error that gets thrown when pushApplicationManifest is invoked
     * @param throwable the exception that should be thrown
     * @return the builder instance
     */
    public ApplicationsMockBuilder setPushApplicationManifestError(Throwable throwable) {
        checkNotNull(apps);

        this.pushAppManifestException = throwable;
        return this;
    }

    /**
     * @return a mock of the {@link Applications}
     */
    public Applications build() {
        Applications applicationsMock = mock(Applications.class);

        mockList(applicationsMock);
        mockGetApplicationManifest(applicationsMock);
        mockPushManifest(applicationsMock);
        mockDelete(applicationsMock);
        mockGet(applicationsMock);

        return applicationsMock;
    }

    private void mockGet(Applications applicationsMock) {
        when(applicationsMock.get(any(GetApplicationRequest.class)))
                .thenAnswer(invocation -> {
                    GetApplicationRequest request = invocation.getArgument(0);

                    Map.Entry<String, ApplicationManifest> appManifestEntry = this.apps.entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().getName().equals(request.getName()))
                            .findFirst()
                            .get();
                    return Mono.just(toApplicationDetail(appManifestEntry.getKey(), appManifestEntry.getValue()));
                });
    }

    private void mockDelete(Applications applicationsMock) {
        when(applicationsMock.delete(any(DeleteApplicationRequest.class)))
                .thenReturn(Mono.just(mock(Void.class)));
    }

    private void mockPushManifest(Applications applicationsMock) {
        if (pushAppManifestException == null) {
            when(applicationsMock.pushManifest(any(PushApplicationManifestRequest.class)))
                    .thenReturn(Mono.just(mock(Void.class)));
        } else {
            when(applicationsMock.pushManifest(any(PushApplicationManifestRequest.class)))
                    .thenReturn(Mono.error(pushAppManifestException));
        }
    }

    private void mockGetApplicationManifest(Applications applicationsMock) {
        /**
         * Creates and configures mock object for CF API client
         * We only have to patch it so far as that it will return our own list of ApplicationSummary instances
         * @param appSummaries List of ApplicationSummary objects that the mock object shall return
         * @return mock {@link DefaultCloudFoundryOperations} object
         */
        when(applicationsMock.getApplicationManifest(any(GetApplicationManifestRequest.class)))
                .thenAnswer(invocation -> {
                    GetApplicationManifestRequest request = invocation.getArgument(0);

                    // simple linear search; this is not about performance, really
                    if(this.apps.containsKey(request.getName())) {
                        return Mono.just(this.apps.get(request.getName()));
                    }

                    throw new RuntimeException("App does not exist.");
                });
    }

    private void mockList(Applications applicationsMock) {
        when(applicationsMock.list())
                .thenReturn(Flux.fromIterable(apps.entrySet()
                        .stream()
                        .map(entry -> toApplicationSummary(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList())));
    }

    private ApplicationSummary toApplicationSummary(String appId ,ApplicationManifest appManifest) {
        return ApplicationSummary.builder()
                // TODO id, requestedState and runningInstances are just dummy values
                .id(appId)
                .runningInstances(0)
                .requestedState("some state")
                .diskQuota(appManifest.getDisk())
                .memoryLimit(appManifest.getMemory())
                .instances(appManifest.getInstances())
                .name(appManifest.getName())
                .build();
    }

    private ApplicationDetail toApplicationDetail(String appId, ApplicationManifest appManifest) {
        return ApplicationDetail.builder()
                .id(appId)
                .requestedState("some state")
                .buildpack(appManifest.getBuildpack())
                .diskQuota(appManifest.getDisk())
                .memoryLimit(appManifest.getMemory())
                .name(appManifest.getName())
                .stack(appManifest.getStack())
                .instances(appManifest.getInstances())
                .runningInstances(0)
                .build();
    }
}
