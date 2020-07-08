package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryChange;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class parses change objects of type {@link MapChange} to a single
 * custom change object of type {@link CfMapChange} returned as list
 */
public class MapChangeParsingStrategy extends AbstractParsingStrategy {

    @Override
    protected List<CfChange> doParse(Change change) {
        MapChange mapChange = (MapChange) change;

        List<CfMapValueChanged> cfChanges = mapChange.getEntryChanges()
                .stream()
                .map(this::parseMapEntry)
                .collect(Collectors.toList());

        return Arrays.asList(new CfMapChange(change.getAffectedObject().get(),
                mapChange.getPropertyName(),
                extractPathFrom(change),
                cfChanges));
    }

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(MapChange.class);
    }

    private CfMapValueChanged parseMapEntry(EntryChange entryChange) {
        if (entryChange instanceof EntryAdded) {
            return new CfMapValueChanged(entryChange.getKey().toString(),
                    "",
                    ((EntryAdded) entryChange).getValue().toString(),
                    ChangeType.ADDED);
        } else if ( entryChange instanceof EntryRemoved) {
            return new CfMapValueChanged(entryChange.getKey().toString(),
                    ((EntryRemoved) entryChange).getValue().toString(),
                    "" ,
                    ChangeType.REMOVED);
        } else {
            return new CfMapValueChanged(entryChange.getKey().toString(),
                    ((EntryValueChange) entryChange).getLeftValue().toString(),
                    ((EntryValueChange) entryChange).getRightValue().toString(),
                    ChangeType.CHANGED);
        }
    }
}
