package cloud.foundry.cli.logic.diff.change;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChangeValue;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChangeValue;
import cloud.foundry.cli.logic.diff.change.object.CfObjectChange;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChange;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryChange;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class parses JaVers change objects to custom change objects
 */
public class ChangeParser {

    /**
     * Parse the JaVers change class to a more appropriate custom change class
     * @param change the JaVers change class
     * @return custom change class
     */
    public static CfChange parse(Change change) {
        if (change instanceof ValueChange) {
            return parseValueChange((ValueChange)change);
        } else if (change instanceof MapChange) {
            return parseMapChange((MapChange) change);
        } else if (change instanceof ContainerChange) {
            return parseContainerChange((ContainerChange) change);
        } else if (change instanceof ObjectRemoved) {
            return new CfObjectChange(change.getAffectedObject().get(), ChangeType.REMOVED);
        } else if (change instanceof NewObject) {
            return new CfObjectChange(change.getAffectedObject().get(), ChangeType.ADDED);
        }
        Log.info("Change type " + change.getClass() + " is not supported for parsing. Ignoring it.");
        return null;
    }

    private static CfChange parseValueChange(ValueChange change) {
        return new CfObjectValueChange(change.getAffectedObject().get(),
                change.getLeft() == null ? "" : change.getLeft().toString(),
                change.getRight() == null ? "" : change.getRight().toString(),
                change.getPropertyName());
    }

    private static CfMapChange parseMapChange(MapChange change) {
        List<CfMapChangeValue> cfChanges = change.getEntryChanges()
                .stream()
                .map(ChangeParser::parseMapEntry)
                .collect(Collectors.toList());
        return new CfMapChange(change.getAffectedObject().get(), change.getPropertyName(), cfChanges);
    }

    private static CfMapChangeValue parseMapEntry(EntryChange entryChange) {
        if (entryChange instanceof EntryAdded) {
            return new CfMapChangeValue(entryChange.getKey().toString(),
                    "",
                    ((EntryAdded) entryChange).getValue().toString(),
                    ChangeType.ADDED);
        } else if ( entryChange instanceof EntryRemoved) {
            return new CfMapChangeValue(entryChange.getKey().toString(),
                    ((EntryRemoved) entryChange).getValue().toString(),
                    "" ,
                    ChangeType.REMOVED);
        } else {
            return new CfMapChangeValue(entryChange.getKey().toString(),
                    ((EntryValueChange) entryChange).getLeftValue().toString(),
                    ((EntryValueChange) entryChange).getRightValue().toString(),
                    ChangeType.CHANGED);
        }
    }

    private static CfChange parseContainerChange(ContainerChange change) {
        List<CfContainerChangeValue> cfChanges = change.getChanges()
                .stream()
                .map(ChangeParser::parseListEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new CfContainerChange(change.getAffectedObject().get(), change.getPropertyName(), cfChanges);
    }

    private static CfContainerChangeValue parseListEntry(ContainerElementChange elementChange) {
        if (elementChange instanceof ValueAdded) {
            return new CfContainerChangeValue(((ValueAdded) elementChange).getAddedValue().toString(),
                    ChangeType.ADDED);
        } else if ( elementChange instanceof ValueRemoved) {
            return new CfContainerChangeValue(((ValueRemoved) elementChange).getRemovedValue().toString(),
                    ChangeType.REMOVED);
        }
        Log.warn("List change type not supported: " + elementChange.getClass());
        return null;
    }
}
