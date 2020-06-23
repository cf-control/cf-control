package cloud.foundry.cli.services;

import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Common options for the initialization process of the
 * {@link org.cloudfoundry.operations.CloudFoundryOperations operations} object.
 *
 * @see cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator
 */
public class LoginCommandOptions {

    @Option(names = { "-u", "--user" }, required = false, scope = ScopeType.INHERIT,
        description = "Your account's e-mail address or username.")
    String userName;

    @Option(names = { "-p", "--password" }, required = false, scope = ScopeType.INHERIT,
        description = "Your password of your cf account.")
    String password;

    @Option(names = { "-a", "--api" }, required = false, scope = ScopeType.INHERIT,
        description = "Your CF instance's API endpoint URL.")
    String apiHost;

    @Option(names = { "-o", "--organization" }, required = false, scope = ScopeType.INHERIT,
        description = "Your CF organization's name.")
    String organization;

    @Option(names = { "-s", "--space" }, required = false, scope = ScopeType.INHERIT,
        description = "Your CF space name.")
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
