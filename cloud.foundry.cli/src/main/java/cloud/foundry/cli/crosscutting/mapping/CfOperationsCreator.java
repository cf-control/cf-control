package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.MissingCredentialsException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.services.LoginCommandOptions;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;

public class CfOperationsCreator {

    private static final Log log = Log.getLog(CfOperationsCreator.class);

    /**
     * Names of the environment variables that hold the username and the password value for the application.
     */
    private static final String CF_CONTROL_USER = "CF_CONTROL_USER";
    private static final String CF_CONTROL_PASSWORD = "CF_CONTROL_PASSWORD";

    /**
     * Creates the cfOperations object, which is needed by our operations classes in the package.
     * {@link cloud.foundry.cli.operations}. The cfOperations object
     *
     * @param commandOptions {@link LoginCommandOptions}
     * @return DefaultCloudFoundryOperations object, which is the entry point for accessing
     * the CF configurations.
     * @throws MissingCredentialsException if either the username or the password cannot be determined
     */
    public static DefaultCloudFoundryOperations createCfOperations(LoginCommandOptions commandOptions) {
        log.debug("Create the cfOperations object with your login command options...");

        DefaultConnectionContext connectionContext = createConnectionContext(commandOptions);
        PasswordGrantTokenProvider tokenProvider = createTokenProvider(commandOptions);
        ReactorCloudFoundryClient cfClient = createCloudFoundryClient(connectionContext, tokenProvider);
        ReactorDopplerClient reactorDopplerClient = createReactorDopplerClient(connectionContext, tokenProvider);
        ReactorUaaClient reactorUaaClient = createReactorUaaClient(connectionContext, tokenProvider);

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

    private static PasswordGrantTokenProvider createTokenProvider(LoginCommandOptions commandOptions) {

        String user = commandOptions.getUserName();
        if (user == null) {
            user = System.getenv(CF_CONTROL_USER);
        }

        String password = commandOptions.getPassword();
        if (password == null) {
            password = System.getenv(CF_CONTROL_PASSWORD);
        }

        if (user == null || password == null) {
            throw new MissingCredentialsException(user, password);
        }

        return PasswordGrantTokenProvider.builder()
                .username(user)
                .password(password)
                .build();
    }

    private static DefaultConnectionContext createConnectionContext(LoginCommandOptions commandOptions) {
        return DefaultConnectionContext.builder()
                .apiHost(commandOptions.getApiHost())
                .build();
    }
}
