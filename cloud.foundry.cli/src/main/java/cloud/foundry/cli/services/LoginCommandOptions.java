package cloud.foundry.cli.services;

import picocli.CommandLine.Option;

/**
 * Common options for the initialization process of the
 * {@link org.cloudfoundry.operations.CloudFoundryOperations operations} object.
 *
 * @see cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator
 */
public class LoginCommandOptions {

    @Option(names = {"-u", "--user"}, required = false, description = "Your account's e-mail address or username.")
    String userName;

    @Option(names = {"-p", "--password"}, required = false, description = "Your password of your cf account.")
    String password;

    @Option(names = {"-a", "--api"}, required = true, description = "Your CF instance's API endpoint URL.")
    String apiHost;

    @Option(names = {"-o", "--organization"}, required = true, description = "The name of your org in cf.")
    String organization;

    @Option(names = {"-s", "--space"}, required = true, description = "The space of cf.")
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