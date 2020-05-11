package cloud.foundry.cli.getservice;

import picocli.CommandLine;

public class GetServiceCommandOptions {
    @CommandLine.Option(names = {"-u", "--user"}, required = false)
    String userName;

    @CommandLine.Option(names = {"-p", "--password"}, required = false)
    String password;

    @CommandLine.Option(names = {"-a", "--api"}, required = false)
    String apiHost;

    @CommandLine.Option(names = {"-o", "--organization"}, required = false)
    String organization;

    @CommandLine.Option(names = {"-s", "--space"}, required = true)
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
