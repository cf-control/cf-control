package cloud.foundry.cli.services;

import picocli.CommandLine.Option;

/**
 * Common options for the initialization process of the {@link org.cloudfoundry.operations.CloudFoundryOperations operations} object.
 *
 * @see cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator
 */
public class LoginCommandOptions {

    @Option(names = {"-u", "--user"}, required = false)
    String userName;

    @Option(names = {"-p", "--password"}, required = false)
    String password;

    @Option(names = {"-a", "--api"}, required = true)
    String apiHost;

    @Option(names = {"-o", "--organization"}, required = true)
    String organization;

    @Option(names = {"-s", "--space"}, required = true)
    String space;

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getApiHost() {
        return apiHost;
    }

    public String getOrganization() {
        return organization;
    }

    public String getSpace() {
        return space;
    }
}