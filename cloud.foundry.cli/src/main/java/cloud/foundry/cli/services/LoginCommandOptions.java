package cloud.foundry.cli.services;

/**
 * Interface for the user provided login options.
 */
public interface LoginCommandOptions {

    String getApiHost();

    String getSpace();

    String getOrganization();

    String getUserName();

    String getPassword();
}
