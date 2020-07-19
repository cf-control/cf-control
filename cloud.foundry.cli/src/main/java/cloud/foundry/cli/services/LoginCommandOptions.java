package cloud.foundry.cli.services;

public interface LoginCommandOptions {

    String getApiHost();

    String getSpace();

    String getOrganization();

    String getUserName();

    String getPassword();
}
