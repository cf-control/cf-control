package cloud.foundry.cli.crosscutting.mapping;

import static org.apache.commons.lang3.StringUtils.isBlank;

import cloud.foundry.cli.crosscutting.exceptions.MissingCredentialsException;
import cloud.foundry.cli.crosscutting.exceptions.MissingTargetInformationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import cloud.foundry.cli.services.LoginCommandOptions;
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
     * @param commandOptions {@link LoginCommandOptions}
     * @param targetBean     Bean that holds all data that is related to the target space (can be null).
     * @return DefaultCloudFoundryOperations object, which is the entry point for accessing
     * the CF configurations.
     * @throws MissingCredentialsException       if either the username or the password cannot be determined
     * @throws MissingTargetInformationException if the target information is not fully specified
     */
    public static DefaultCloudFoundryOperations createCfOperations(TargetBean targetBean,
                                                                   LoginCommandOptions commandOptions) {

        log.debug("Setting up CF operations object with your login command options");

        targetBean = replaceTargetOptions(targetBean, commandOptions);
        checkTargetIsFullySpecified(targetBean);

        DefaultConnectionContext connectionContext = createConnectionContext(targetBean.getEndpoint());
        PasswordGrantTokenProvider tokenProvider = createTokenProvider(commandOptions);
        ReactorCloudFoundryClient cfClient = createCloudFoundryClient(connectionContext, tokenProvider);
        ReactorDopplerClient reactorDopplerClient = createReactorDopplerClient(connectionContext, tokenProvider);
        ReactorUaaClient reactorUaaClient = createReactorUaaClient(connectionContext, tokenProvider);

        return DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient)
                .dopplerClient(reactorDopplerClient)
                .uaaClient(reactorUaaClient)
                .organization(targetBean.getOrg())
                .space(targetBean.getSpace())
                .build();
    }

    private static TargetBean replaceTargetOptions(TargetBean targetBean, LoginCommandOptions commandOptions) {
        // as the passed target bean can be null, a new instance is then created to avoid null pointer exceptions
        if (targetBean == null) {
            targetBean = new TargetBean();
        }

        // create a new resulting target bean instance in order to avoid manipulating the passed target bean instance
        TargetBean result = new TargetBean();
        result.setEndpoint(targetBean.getEndpoint());
        result.setOrg(targetBean.getOrg());
        result.setSpace(targetBean.getSpace());

        String apiHost = commandOptions.getApiHost();
        String organization = commandOptions.getOrganization();
        String space = commandOptions.getSpace();

        if (!isBlank(apiHost)) {
            logReplacingTargetOption("endpoint", targetBean.getEndpoint(), apiHost);
            result.setEndpoint(apiHost);
        }
        if (!isBlank(organization)) {
            logReplacingTargetOption("organization", targetBean.getOrg(), apiHost);
            result.setOrg(organization);
        }
        if (!isBlank(space)) {
            logReplacingTargetOption("space", targetBean.getSpace(), apiHost);
            result.setSpace(space);
        }

        return result;
    }

    private static void logReplacingTargetOption(String informationName, String targetValue, String argumentValue) {
        if (targetValue != null) {
            log.verbose("Replacing the " + informationName + " '" + targetValue + "' from the target section " +
                    "with the " + informationName + " '" + argumentValue + "' specified in the commandline arguments");
        }
    }

    private static void checkTargetIsFullySpecified(TargetBean targetBean) {
        boolean isTargetUnderspecified = Stream.of(targetBean.getEndpoint(), targetBean.getOrg(), targetBean.getSpace())
                .anyMatch(StringUtils::isBlank);
        if (isTargetUnderspecified) {
            throw new MissingTargetInformationException(targetBean);
        }
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

    private static DefaultConnectionContext createConnectionContext(String apiHost) {
        return DefaultConnectionContext.builder()
                .apiHost(apiHost)
                .build();
    }
}
