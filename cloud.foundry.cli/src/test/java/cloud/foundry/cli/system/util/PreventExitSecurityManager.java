package cloud.foundry.cli.system.util;

import java.security.Permission;

/**
 * Custom security manager which prevents the JVM from exiting.
 * Instead, it raises a {@link SystemExitException} when there's a call to System.exit(...).
 */
public class PreventExitSecurityManager extends SecurityManager {
    // these checkPermission overrides are needed for some reason...
    @Override
    public void checkPermission(Permission perm) {}

    @Override
    public void checkPermission(Permission perm, Object context) {}

    @Override
    public void checkExit(int status) {
        super.checkExit(status);
        throw new SystemExitException(status);
    }
}
