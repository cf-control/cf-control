package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;

/**
 * TODO doc
 */
public class ApplicationDiff extends AbstractDiff<ApplicationBean> {

    private ApplicationManifestDiff manifestDifference;

    /**
     * TODO doc
     */
    public ApplicationDiff(ApplicationBean affected, ApplicationManifestDiff manifestDifference) {
        super(affected);
        this.manifestDifference = manifestDifference;
    }

    public ApplicationManifestDiff getManifestDifference() {
        return manifestDifference;
    }
}
