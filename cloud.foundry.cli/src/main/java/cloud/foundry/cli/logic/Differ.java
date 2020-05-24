package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.Bean;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;

import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * TODO doc
 */
public class Differ {

    private static final Javers JAVERS = JaversBuilder.javers().build();

    /**
     * TODO doc
     */
    public static Set<ApplicationDiff> diffApplications(Set<ApplicationBean> presentApplications,
                                                        Set<ApplicationBean> desiredApplications) {
        Diff diff = JAVERS.compareCollections(presentApplications, desiredApplications, ApplicationBean.class);

        List<Change> changes = diff.getChanges();
        ApplicationDiffVisitor visitor = new ApplicationDiffVisitor();
        for (Change change : changes) {
            Optional<Object> possiblyAffectedByChange = change.getAffectedObject();

            // this should always be guaranteed by the diff
            assert possiblyAffectedByChange.isPresent();

            Object affectedByChange = possiblyAffectedByChange.get();
            if (affectedByChange instanceof Bean) {
                Bean affectedBean = (Bean) affectedByChange;
                visitor.setCurrentChange(change);
                affectedBean.visit(visitor);
            }
        }
        return visitor.getApplicationDiffs();
    }
}
