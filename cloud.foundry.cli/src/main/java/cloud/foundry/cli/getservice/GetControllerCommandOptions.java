package cloud.foundry.cli.getservice;

import picocli.CommandLine.Option;

/**
 * Common Options for the Get-Controller Sub commands.
 *
 * @see GetController
 */
public class GetControllerCommandOptions {

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