package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.beans.BeanVisitor;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.javers.core.diff.Change;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO doc
 */
public class ApplicationDiffVisitor implements BeanVisitor {

    // this is used to remember some already visited beans and already created diff objects
    private Map<ApplicationManifestBean, ApplicationDiff> applicationDifferenceByManifestBean;

    // holds a change of the currently visited bean
    private Change changeOfCurrentBean;

    public ApplicationDiffVisitor() {
        changeOfCurrentBean = null;
        applicationDifferenceByManifestBean = new HashMap<>();
    }

    public void setCurrentChange(Change change) {
        this.changeOfCurrentBean = change;
    }

    public Set<ApplicationDiff> getApplicationDiffs() {
        return new HashSet<>(applicationDifferenceByManifestBean.values());
    }

    @Override
    public void visit(GetAllBean getAllBean) {

    }

    /**
     * TODO doc
     */
    @Override
    public void visit(ApplicationBean applicationBean) {
        assertChangeWasSet();

        ApplicationManifestBean manifestBean = applicationBean.getManifest();
        ApplicationDiff applicationDifference = applicationDifferenceByManifestBean.get(manifestBean);

        if (applicationDifference == null) {
            ApplicationManifestDiff manifestDifference = new ApplicationManifestDiff(manifestBean);
            applicationDifference = new ApplicationDiff(applicationBean, manifestDifference);
            applicationDifferenceByManifestBean.put(manifestBean, applicationDifference);
        }
        applicationDifference.addChange(changeOfCurrentBean);
    }

    /**
     * TODO doc
     */
    @Override
    public void visit(ApplicationManifestBean manifestBean) {
        assertChangeWasSet();

        ApplicationDiff applicationDifference = applicationDifferenceByManifestBean.get(manifestBean);
        ApplicationManifestDiff manifestDifference;

        if (applicationDifference == null) {
            manifestDifference = new ApplicationManifestDiff(manifestBean);
            ApplicationBean applicationBean = manifestBean.provideLinkedApplicationBean();
            applicationDifference = new ApplicationDiff(applicationBean, manifestDifference);
            applicationDifferenceByManifestBean.put(manifestBean, applicationDifference);
        } else {
            manifestDifference = applicationDifference.getManifestDifference();
        }
        manifestDifference.addChange(changeOfCurrentBean);
    }

    @Override
    public void visit(ServiceBean serviceBean) {

    }

    @Override
    public void visit(SpaceDevelopersBean spaceDevelopersBean) {

    }

    private void assertChangeWasSet() {
        if (changeOfCurrentBean == null) {
            throw new IllegalStateException("The current change for the visited bean is not set");
        }
    }
}
