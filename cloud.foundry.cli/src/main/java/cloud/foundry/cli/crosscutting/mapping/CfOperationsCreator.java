package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.services.LoginCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;

public class CfOperationsCreator {

    public static DefaultCloudFoundryOperations createCfOperations(
            LoginCommandOptions commandOptions) {

        DefaultConnectionContext connectionContext = createConnectionContext(commandOptions);
        PasswordGrantTokenProvider tokenProvider = createTokenProvider(commandOptions);
        ReactorCloudFoundryClient cfClient =
                createCloudFoundryClient(connectionContext, tokenProvider);
        ReactorDopplerClient reactorDopplerClient =
                createReactorDopplerClient(connectionContext, tokenProvider);
        ReactorUaaClient reactorUaaClient =
                createReactorUaaClient(connectionContext, tokenProvider);

        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .dopplerClient(reactorDopplerClient)
                .uaaClient(reactorUaaClient)
                .organization(commandOptions.getOrganization())
                .space(commandOptions.getSpace())
                .build();
    }

    private static ReactorUaaClient createReactorUaaClient(
            DefaultConnectionContext connectionContext,
            PasswordGrantTokenProvider tokenProvider) {

        return ReactorUaaClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    private static ReactorDopplerClient createReactorDopplerClient(
            DefaultConnectionContext connectionContext,
            PasswordGrantTokenProvider tokenProvider) {

        return ReactorDopplerClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    private static ReactorCloudFoundryClient createCloudFoundryClient(
            DefaultConnectionContext connectionContext,
            PasswordGrantTokenProvider tokenProvider) {

        return ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build();
    }

    private static PasswordGrantTokenProvider createTokenProvider(
            LoginCommandOptions commandOptions) {

        return PasswordGrantTokenProvider.builder()
                .password(commandOptions.getPassword())
                .username(commandOptions.getUserName())
                .build();
    }

    private static DefaultConnectionContext createConnectionContext(
            LoginCommandOptions commandOptions) {
        return DefaultConnectionContext.builder()
                .apiHost(commandOptions.getApiHost())
                .build();
    }
}
