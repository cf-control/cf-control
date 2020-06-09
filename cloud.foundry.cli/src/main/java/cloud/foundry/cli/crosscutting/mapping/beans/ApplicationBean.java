package cloud.foundry.cli.crosscutting.mapping.beans;

import org.cloudfoundry.operations.applications.ApplicationManifest;

/**
 * Bean holding all data that is related to an application.
 */
public class ApplicationBean implements Bean {

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
        this.path = manifest.getPath() == null ? null : manifest.getPath().toString();
        this.manifest = new ApplicationManifestBean(manifest);
    }

    public ApplicationBean() {
    }

    @Override
    public String toString() {
        return "ApplicationBean{" +
                "manifest=" + manifest.toString() +
                ", path='" + path + '\'' +
                '}';
    }
}
