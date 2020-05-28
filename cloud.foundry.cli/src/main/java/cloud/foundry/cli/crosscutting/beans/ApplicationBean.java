package cloud.foundry.cli.crosscutting.beans;

import org.cloudfoundry.operations.applications.ApplicationManifest;

/**
 * Bean holding all data that is related to an application.
 */
public class ApplicationBean implements Bean {

    /**
     * TODO : replace this field, since it will be stored in the map in the bean above (SpecBean)
     * TODO : requires adjustment in the applications operations
     */
    private String name;
    private ApplicationManifestBean manifest;
    private String path;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
        this.name = manifest.getName();
        this.path = manifest.getPath() == null ? null : manifest.getPath().toString();
        this.manifest = new ApplicationManifestBean(manifest);
    }

    public ApplicationBean() {
    }

}