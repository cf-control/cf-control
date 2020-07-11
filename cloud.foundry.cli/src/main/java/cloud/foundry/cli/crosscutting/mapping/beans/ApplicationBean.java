package cloud.foundry.cli.crosscutting.mapping.beans;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.javers.core.metamodel.annotation.TypeName;

/**
 * Bean holding all data that is related to an application.
 */
@TypeName("ApplicationBean")
public class ApplicationBean implements Bean {

    /**
     * The key for the custom metadata annotation.
     */
    public static final String METADATA_KEY = "CF_CONTROL_METADATA";

    /**
     * The key for the custom path annotation.
     */
    public static final String PATH_KEY = "CF_CONTROL_PATH";

    /**
     * The key for the custom Docker Image annotation.
     */

    public static final String DOCKER_IMAGE_KEY = "CF_CONTROL_DOCKER_IMAGE";
    /**
     * The key for the custom Docker Username annotation.
     */
    public static final String DOCKER_USERNAME_KEY = "CF_DOCKER_USERNAME";

    private ApplicationManifestBean manifest;
    private String path;
    private String meta;
    private String dockerImage;
    private String dockerUsername;

    public ApplicationBean(ApplicationManifest manifest, Metadata meta) {
        this.path = manifest.getPath() == null ? null : manifest.getPath().toString();
        this.manifest = new ApplicationManifestBean(manifest);
        this.meta = meta.getAnnotations().get(METADATA_KEY);
        this.path = meta.getAnnotations().get(PATH_KEY);
        this.dockerImage =  meta.getAnnotations().get(DOCKER_IMAGE_KEY);
        this.dockerUsername =  meta.getAnnotations().get(DOCKER_USERNAME_KEY);
    }

    public ApplicationBean() {
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

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public String getDockerUsername() {
        return dockerUsername;
    }

    public void setDockerUsername(String dockerUsername) {
        this.dockerUsername = dockerUsername;
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
