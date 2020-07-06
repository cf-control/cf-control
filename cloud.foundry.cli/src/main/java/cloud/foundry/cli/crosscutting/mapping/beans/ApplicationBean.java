package cloud.foundry.cli.crosscutting.mapping.beans;

import org.cloudfoundry.operations.applications.ApplicationManifest;

/**
 * Bean holding all data that is related to an application.
 */
public class ApplicationBean implements Bean {

    private ApplicationManifestBean manifest;
    private String path;
    private String meta;

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

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public ApplicationBean(ApplicationManifest manifest, String meta) {
        this.path = manifest.getPath() == null ? null : manifest.getPath().toString();
        this.manifest = new ApplicationManifestBean(manifest);
        this.meta = meta;
    }

    public ApplicationBean() {
    }

    @Override
    public String toString() {
        return "ApplicationBean{" +
                "manifest=" + manifest +
                ", path='" + path + '\'' +
                ", meta='" + meta + '\'' +
                '}';
    }
}
