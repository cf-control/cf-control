package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.crosscutting.beans.BeanVisitor;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

/**
 * TODO doc
 */
public class DifferenceCreator implements BeanVisitor {

    // this is only needed for application difference creation
    private Map<ApplicationManifestBean, ApplicationDifference> applicationDifferenceByManifestBean;

    // holds a change of the currently visited bean
    private Change changeOfCurrentBean;

    private static final Javers JAVERS = JaversBuilder.javers().build();

    /**
     * TODO doc
     */
    public Set<ApplicationDifference> createApplicationDifference(Set<ApplicationBean> presentApplications,
                                                                  Set<ApplicationBean> desiredApplications) {
        Diff diff = JAVERS.compareCollections(presentApplications, desiredApplications, ApplicationBean.class);

        applicationDifferenceByManifestBean = new HashMap<>();

        List<Change> changes = diff.getChanges();
        for (Change change : changes) {

            // store the change for the current object in an attribute to make the changes accessible when visiting the
            // bean object
            changeOfCurrentBean = change;

            Optional<Object> possiblyAffectedByChange = change.getAffectedObject();

            // this should always be guaranteed by the diff
            assert possiblyAffectedByChange.isPresent();

            Object affectedByChange = possiblyAffectedByChange.get();
            if (affectedByChange instanceof Bean) {
                Bean affectedBean = (Bean) affectedByChange;
                affectedBean.visit(this);
            }
            // ignore all changes that affect non-bean objects because those kinds of changes are not relevant for the
            // purpose of this class
        }
        return new HashSet<>(applicationDifferenceByManifestBean.values());
    }

    @Override
    public void visit(GetAllBean getAllBean) {
        //TODO has to be implemented for diffing a full configuration
    }

    /**
     * TODO doc
     */
    @Override
    public void visit(ApplicationBean applicationBean) {
        ApplicationManifestBean manifestBean = applicationBean.getManifest();
        ApplicationDifference applicationDifference = applicationDifferenceByManifestBean.get(manifestBean);

        if (applicationDifference == null) {
            ApplicationManifestDifference manifestDifference = new ApplicationManifestDifference(manifestBean);
            applicationDifference = new ApplicationDifference(applicationBean, manifestDifference);
            applicationDifferenceByManifestBean.put(manifestBean, applicationDifference);
        }
        applicationDifference.addChange(changeOfCurrentBean);
    }

    /**
     * TODO doc
     */
    @Override
    public void visit(ApplicationManifestBean manifestBean) {
        ApplicationDifference applicationDifference = applicationDifferenceByManifestBean.get(manifestBean);
        ApplicationManifestDifference manifestDifference;

        if (applicationDifference == null) {
            manifestDifference = new ApplicationManifestDifference(manifestBean);
            ApplicationBean applicationBean = manifestBean.provideLinkedApplicationBean();
            applicationDifference = new ApplicationDifference(applicationBean, manifestDifference);
            applicationDifferenceByManifestBean.put(manifestBean, applicationDifference);
        } else {
            manifestDifference = applicationDifference.getManifestDifference();
        }
        manifestDifference.addChange(changeOfCurrentBean);
    }

    @Override
    public void visit(ServiceBean serviceBean) {
        //TODO has to be implemented for diffing services
    }

    @Override
    public void visit(SpaceDevelopersBean spaceDevelopersBean) {
        //TODO has to be implemented for diffing space developers
    }
}
