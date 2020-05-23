package cloud.foundry.cli.crosscutting.beans;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.javers.core.metamodel.annotation.Id;

import java.util.List;

/**
 * Bean holding all data that is related to an application.
 */
public class ApplicationBean implements Bean {

    private String id;
    private String name;
    private List<String> urls;
    private int instances;
    private int runningInstances;
    private int memoryLimit;
    private int diskQuota;
    private String requestedState;
    private ApplicationManifestBean manifest;
    @Id
    private String path;


    public ApplicationManifestBean getManifest() {
        return manifest;
    }

    public void setManifest(ApplicationManifestBean manifestBean) {
        this.manifest = manifestBean;
        manifest.linkApplicationBean(this);
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

    @Override
    public void visit(BeanVisitor visitor) {
        visitor.visit(this);
    }
}