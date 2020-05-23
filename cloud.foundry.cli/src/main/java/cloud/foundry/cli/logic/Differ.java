package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;

import java.util.Set;

/**
 * TODO doc
 */
public class Differ {

    /**
     * TODO doc
     */
    public static Set<ApplicationDifference> diffApplications(Set<ApplicationBean> presentApplications,
                                                              Set<ApplicationBean> desiredApplications) {
        DifferenceCreator differenceCreator = new DifferenceCreator();
        return differenceCreator.createApplicationDifference(presentApplications, desiredApplications);
    }
}
