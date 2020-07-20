package cloud.foundry.cli.logic.diff.change.parsing;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryChange;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class parses change objects of type {@link MapChange} to a single
 * custom change object of type {@link CfMapChange} returned as list
 */
public class MapChangeParsingStrategy extends AbstractParsingStrategy {

    private static final Log log = Log.getLog(MapChangeParsingStrategy.class);

    @Override
    protected List<CfChange> doParse(Change change) {
        MapChange mapChange = (MapChange) change;

        log.verbose("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfMapChange.class.getSimpleName());
        List<CfMapValueChanged> cfChanges = mapChange.getEntryChanges()
                .stream()
                .map(entryChange -> parseMapEntry(((MapChange) change).getPropertyName(), entryChange))
                .collect(Collectors.toList());

        log.debug("Parsing change type", change.getClass().getSimpleName(), "to custom change type",
                CfMapChange.class.getSimpleName(), "completed");
        return Collections.singletonList(new CfMapChange(change.getAffectedObject().get(),
                mapChange.getPropertyName(),
                extractPathFrom(change),
                cfChanges));
    }

    @Override
    public List<Class<? extends Change>> getMatchingTypes() {
        return Arrays.asList(MapChange.class);
    }

    private CfMapValueChanged parseMapEntry(String propertyName, EntryChange entryChange) {
        if (entryChange instanceof EntryAdded) {
            log.debug("Appending", propertyName,"map change with added entry:",
                    entryChange.getKey() + ":", ((EntryAdded) entryChange).getValue());

            return new CfMapValueChanged(entryChange.getKey().toString(),
                    "",
                    ((EntryAdded) entryChange).getValue().toString(),
                    ChangeType.ADDED);
        } else if ( entryChange instanceof EntryRemoved) {
            log.debug("Appending", propertyName,"map change with removed entry:",
                    entryChange.getKey() + ":", ((EntryRemoved) entryChange).getValue());

            return new CfMapValueChanged(entryChange.getKey().toString(),
                    ((EntryRemoved) entryChange).getValue().toString(),
                    "" ,
                    ChangeType.REMOVED);
        } else {
            log.debug("Appending", propertyName,"map change with changed entry:",
                    entryChange.getKey() + ":", ((EntryValueChange) entryChange).getLeftValue(), "to",
                    entryChange.getKey() + ":",((EntryValueChange) entryChange).getRightValue());

            return new CfMapValueChanged(entryChange.getKey().toString(),
                    ((EntryValueChange) entryChange).getLeftValue().toString(),
                    ((EntryValueChange) entryChange).getRightValue().toString(),
                    ChangeType.CHANGED);
        }
    }
}
