package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;

/**
 * TODO doc
 */
public class ApplicationDifference extends AbstractDifference<ApplicationBean> {

    private ApplicationManifestDifference manifestDifference;

    /**
     * TODO doc
     */
    public ApplicationDifference(ApplicationBean affected, ApplicationManifestDifference manifestDifference) {
        super(affected);
        this.manifestDifference = manifestDifference;
    }

    public ApplicationManifestDifference getManifestDifference() {
        return manifestDifference;
    }
}
