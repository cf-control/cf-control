package cloud.foundry.cli.crosscutting;

/**
 * Set of common precondition checks for use in our project.
 *
 * The methods can be imported statically.
 */
public final class Preconditions {

    private static final String ERROR_MESSAGE = "precondition failed";

    /**
     * Check whether an object is not null. Throws an exception in case the checks fails.
     * @param object object to check
     * @throws NullPointerException in case the passed object is null
     */
    public static void checkNotNull(Object object) {
        if (object == null) {
            throw new NullPointerException(ERROR_MESSAGE);
        }
    }

}
