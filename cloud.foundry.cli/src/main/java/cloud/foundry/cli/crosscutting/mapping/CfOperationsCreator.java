package cloud.foundry.cli.crosscutting.mapping;

import static org.apache.commons.lang3.StringUtils.isBlank;

import cloud.foundry.cli.crosscutting.exceptions.MissingCredentialsException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.services.ILoginCommandOptions;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;

import java.util.stream.Stream;

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
     * @param commandOptions {@link ILoginCommandOptions}
     * @param targetBean     Bean that holds all data that is related to the target space.
     * @return DefaultCloudFoundryOperations object, which is the entry point for accessing
     * the CF configurations.
     * @throws MissingCredentialsException if either the username or the password cannot be determined
     * @throws NullPointerException if the {@link TargetBean} is null and the {@link ILoginCommandOptions} contains
     * at least one option which is null or empty.
     * @throws IllegalArgumentException if at least one option value
     * could not be determined (value is null or empty) in the {@link ILoginCommandOptions} and {@link TargetBean}.
     */
    public static DefaultCloudFoundryOperations createCfOperations(TargetBean targetBean,
                                                                   ILoginCommandOptions commandOptions) {

        log.debug("Setting up CF operations object with your login command options");

        String apiHost = commandOptions.getApiHost();
        String space = commandOptions.getSpace();
        String organization = commandOptions.getOrganization();

        boolean isOneOptionNullOrEmpty = Stream.of(apiHost, space, organization).anyMatch(StringUtils::isBlank);
        if (isOneOptionNullOrEmpty && targetBean == null) {
            throw new IllegalArgumentException();
        }

        apiHost = isBlank(apiHost) ? determineOptionValue(targetBean.getEndpoint()) : apiHost;
        space = isBlank(space) ? determineOptionValue(targetBean.getSpace()) : space;
        organization = isBlank(organization) ? determineOptionValue(targetBean.getOrg()) : organization;

        DefaultConnectionContext connectionContext = createConnectionContext(apiHost);
        PasswordGrantTokenProvider tokenProvider = createTokenProvider(commandOptions);
        ReactorCloudFoundryClient cfClient = createCloudFoundryClient(connectionContext, tokenProvider);
        ReactorDopplerClient reactorDopplerClient = createReactorDopplerClient(connectionContext, tokenProvider);
        ReactorUaaClient reactorUaaClient = createReactorUaaClient(connectionContext, tokenProvider);

        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .dopplerClient(reactorDopplerClient)
                .uaaClient(reactorUaaClient)
                .organization(organization)
                .space(space)
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

    private static PasswordGrantTokenProvider createTokenProvider(ILoginCommandOptions commandOptions) {
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

    private static DefaultConnectionContext createConnectionContext(String apiHost) {
        return DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();
    }

    private static String determineOptionValue(String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException();
        } else {
            return value;
        }
    }
}
