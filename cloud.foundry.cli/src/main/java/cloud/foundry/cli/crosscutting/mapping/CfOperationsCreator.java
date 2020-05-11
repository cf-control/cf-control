package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.getservice.GetServiceCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;


public class CfOperationsCreator {

    public static DefaultCloudFoundryOperations createCfOperations(
            GetServiceCommandOptions commandOptions) {

        DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                .apiHost(commandOptions.getApiHost())
                .build();
        PasswordGrantTokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                .password(commandOptions.getPassword())
                .username(commandOptions.getUserName())
                .build();
        ReactorCloudFoundryClient cfClient = ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
        ReactorDopplerClient reactorDopplerClient = ReactorDopplerClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
        ReactorUaaClient reactorUaaClient = ReactorUaaClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();

        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .dopplerClient(reactorDopplerClient)
                .uaaClient(reactorUaaClient)
                .organization(commandOptions.getOrganization())
                .space(commandOptions.getSpace())
                .build();
    }
}
