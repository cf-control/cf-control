package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.beans.Bean;
import org.javers.common.string.PrettyPrintBuilder;
import org.javers.common.string.PrettyValuePrinter;
import org.javers.core.Changes;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.map.MapChange;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * TODO doc
 */
public abstract class AbstractDiff<B extends Bean> {

    protected List<Change> changes;
    protected B affected;

    /**
     * TODO doc
     */
    public AbstractDiff(B affected) {
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
