package cloud.foundry.cli.services;

public interface ILoginCommandOptions {

    String getApiHost();

    String getSpace();

    String getOrganization();

    String getUserName();

    String getPassword();
}
