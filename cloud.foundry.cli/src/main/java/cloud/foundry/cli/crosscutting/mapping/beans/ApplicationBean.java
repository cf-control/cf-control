package cloud.foundry.cli.crosscutting.mapping.beans;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.Objects;

/**
 * Bean holding all data that is related to an application.
 */
@TypeName("ApplicationBean")
public class ApplicationBean implements Bean {

    /**
     * Name of the key of annotation for metadata.
     */
    public static final String METADATA_KEY = "CF_METADATA_KEY";

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

    public ApplicationBean(ApplicationManifest manifest, Metadata meta) {
        this.path = manifest.getPath() == null ? null : manifest.getPath().toString();
        this.manifest = new ApplicationManifestBean(manifest);
        this.meta = meta.getAnnotations().get(METADATA_KEY);
        this.path = meta.getAnnotations().get("path");
    }

    public ApplicationBean() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationBean that = (ApplicationBean) o;
        return Objects.equals(manifest, that.manifest) &&
                Objects.equals(path, that.path) &&
                Objects.equals(meta, that.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manifest, path, meta);
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
