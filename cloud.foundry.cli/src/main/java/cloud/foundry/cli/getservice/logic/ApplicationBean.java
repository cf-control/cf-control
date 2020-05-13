package cloud.foundry.cli.getservice.logic;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;

import java.nio.file.Path;
import java.util.List;

/**
 * Immutable data type used to generate nice YAML output for applications.
 */
public class ApplicationBean {

    private String id;
    private String name;
    private List<String> urls;
    private int instances;
    private int runningInstances;
    private int memoryLimit;
    private int diskQuota;
    private String requestedState;
    private ApplicationManifestBean manifest;
    private String path;


    public ApplicationManifestBean getManifest() {
        return manifest;
    }

    public void setManifest(ApplicationManifestBean manifestBean) {
        this.manifest = manifestBean;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public ApplicationBean(ApplicationManifest manifest) {
        // prevent null pointer errors
        if (manifest.getPath() != null) {
            this.path = manifest.getPath().toString();
        } else {
            this.path = null;
        }

        this.manifest = new ApplicationManifestBean(manifest);
    }

    public ApplicationBean() {
    }

}