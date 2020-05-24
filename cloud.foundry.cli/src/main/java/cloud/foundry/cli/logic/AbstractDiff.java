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

    /**
     * TODO doc
     */
    public NewObject getNewObjectChange() {
        return getSoloChangeByType(NewObject.class);
    }

    /**
     * TODO doc
     */
    public ObjectRemoved getObjectRemovedChange() {
        return getSoloChangeByType(ObjectRemoved.class);
    }

    /**
     * TODO doc
     */
    public List<ContainerChange> getContainerChanges() {
        return getChangesByType(ContainerChange.class);
    }

    /**
     * TODO doc
     */
    public List<MapChange> getMapChanges() {
        return getChangesByType(MapChange.class);
    }

    // there is no getReferenceChange method because we are not interested in reference changes

    /**
     * TODO doc
     */
    public List<ValueChange> getValueChanges() {
        return getChangesByType(ValueChange.class);
    }

    private <C extends Change> C getSoloChangeByType(Class<C> type) {
        List<C> soloChanges = getChangesByType(type);
        if (soloChanges.size() > 1) {
            throw new IllegalStateException("There are multiple changes of the type " + type.getCanonicalName());
        }
        if (soloChanges.size() != changes.size()) {
            throw new IllegalStateException("There are other changes besides the change of type" +
                    type.getCanonicalName());
        }
        if (soloChanges.isEmpty()) {
            return null;
        }
        return soloChanges.get(0);
    }

    private <C extends Change> List<C> getChangesByType(Class<C> type) {
        Changes changesWrapper = new Changes(changes, PrettyValuePrinter.getDefault());
        return changesWrapper.getChangesByType(type);
    }

    private void assertChangeAffectsAffected(Change change) {
        Optional<Object> possiblyAffectedObject = change.getAffectedObject();
        if (!possiblyAffectedObject.isPresent() || !possiblyAffectedObject.get().equals(affected)) {
            throw new IllegalArgumentException("The change instance does not affect the according bean object");
        }
    }
}
