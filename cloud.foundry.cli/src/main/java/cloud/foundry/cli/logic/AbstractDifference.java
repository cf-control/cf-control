package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.Bean;
import org.javers.core.diff.Change;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * TODO doc
 */
public abstract class AbstractDifference<B extends Bean> {

    protected List<Change> changes;
    protected B affected;

    /**
     * TODO doc
     */
    public AbstractDifference(B affected) {
        this.changes = new LinkedList<>();
        this.affected = affected;
    }

    public B getAffected() {
        return affected;
    }

    /**
     * TODO doc
     */
    public void addChange(Change change) {
        assertChangeAffectsAffected(change);
        changes.add(change);
    }

    private void assertChangeAffectsAffected(Change change) {
        Optional<Object> possiblyAffectedObject = change.getAffectedObject();
        if (!possiblyAffectedObject.isPresent() || !possiblyAffectedObject.get().equals(affected)) {
            throw new IllegalArgumentException("The change instance does not affect the according bean object");
        }
    }
}
