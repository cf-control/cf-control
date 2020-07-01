package cloud.foundry.cli.crosscutting.exceptions;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Indicates that the provided credentials are incomplete.
 */
public class MissingCredentialsException extends RuntimeException {

    /**
     * Initializes the exception with an error message that is generated from the provided arguments.
     * Either the username or the password (or both) has to be null.
     *
     * @param username the known username or null if unknown
     * @param password the known password or null if unknown
     * @throws IllegalArgumentException if both arguments are not null
     */
    public MissingCredentialsException(String username, String password) {
        super(determineErrorMessage(username, password));
    }

    private static String determineErrorMessage(String username, String password) {
        checkArgument(username == null || password == null,
                "Either the username or the password has to be null");

        if (username == null && password == null) {
            return "The username and the password are not defined.";
        } else if (username == null) {
            return "The username is not defined.";
        }
        return "The password is not defined.";
    }
}
