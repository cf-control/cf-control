package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.services.LoginCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;

public class CfOperationsCreator {

    /**
     * Names of the environment variables that hold the username and the password value for the application.
     */
    private static final String CF_CONTROL_USER = "CF_CONTROL_USER";
    private static final String CF_CONTROL_PASSWORD = "CF_CONTROL_PASSWORD";

    /**
     * Creates the cfOperations object, which is needed by our operations classes in the package. If the username and
     * password is not set within the commandOptions, then it tries to get the values from the System Environment
     * Variables: "CF_CONTROL_USER" & "CF_CONTROL_PASSWORD"
     * {@link cloud.foundry.cli.operations}. The cfOpeartions object
     * @param commandOptions {@link LoginCommandOptions}
     * @return A DefaultCloudFoundryOperations object, which is the entry point for accessing
     * the CF configurations.
     */
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

        String user = determineCredential(commandOptions.getUserName(), CF_CONTROL_USER);
        String password = determineCredential(commandOptions.getPassword(), CF_CONTROL_PASSWORD);

        return PasswordGrantTokenProvider.builder()
                .username(user)
                .password(password)
                .build();
    }

    private static String determineCredential(String credential, String environmentVariableName) {
        String environmentVariableValue = System.getenv(environmentVariableName);
        return credential != null ? credential : environmentVariableValue;
    }

    private static DefaultConnectionContext createConnectionContext(
            LoginCommandOptions commandOptions) {
        return DefaultConnectionContext.builder()
                .apiHost(commandOptions.getApiHost())
                .build();
    }
}
