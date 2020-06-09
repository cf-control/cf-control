package cloud.foundry.cli.logic.diff.change;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class parses JaVers change objects to custom change objects
 */
public class ChangeParser {

    private static final String ROOT_NAME = "config";
    private static final String ROOT_SYMBOL = "#";
    private static final String PATH_SEPARATOR_SYMBOL = "/";

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
            return new CfRemovedObject(change.getAffectedObject().get(),
                    "",
                    extractPathFrom(change));
        } else if (change instanceof NewObject) {
            return new CfNewObject(change.getAffectedObject().get(),
                    "",
                    extractPathFrom(change));
        }
        Log.info("Change type " + change.getClass() + " is not supported for parsing. Ignoring it.");
        return null;
    }

    private static CfChange parseValueChange(ValueChange change) {
        return new CfObjectValueChanged(change.getAffectedObject().get(),
                change.getPropertyName(),
                extractPathFrom(change),
                ChangeType.CHANGED,
                change.getLeft() == null ? "" : change.getLeft().toString(),
                change.getRight() == null ? "" : change.getRight().toString()
                );
    }

    private static CfMapChange parseMapChange(MapChange change) {
        List<CfMapValueChanged> cfChanges = change.getEntryChanges()
                .stream()
                .map(ChangeParser::parseMapEntry)
                .collect(Collectors.toList());

        return new CfMapChange(change.getAffectedObject().get(),
                change.getPropertyName(),
                Collections.emptyList(),
                cfChanges);
    }

    private static CfMapValueChanged parseMapEntry(EntryChange entryChange) {
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

    private static CfChange parseContainerChange(ContainerChange change) {
        List<CfContainerValueChanged> cfChanges = change.getChanges()
                .stream()
                .map(ChangeParser::parseListEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new CfContainerChange(change.getAffectedObject().get(),
                change.getPropertyName(),
                Collections.emptyList(),
                cfChanges);
    }

    private static CfContainerValueChanged parseListEntry(ContainerElementChange elementChange) {
        if (elementChange instanceof ValueAdded) {
            return new CfContainerValueChanged(((ValueAdded) elementChange).getAddedValue().toString(),
                    ChangeType.ADDED);
        } else if ( elementChange instanceof ValueRemoved) {
            return new CfContainerValueChanged(((ValueRemoved) elementChange).getRemovedValue().toString(),
                    ChangeType.REMOVED);
        }
        Log.warn("List change type not supported: " + elementChange.getClass());
        return null;
    }

    /**
     * for example:
     * change.getAffectedGlobalId() = cloud.foundry.cli.crosscutting.bean.ConfigBean/#spec/apps/someApp/manifest
     *                           -> [cloud.foundry.cli.crosscutting.bean.ConfigBean, spec, apps, someApp, manifest]
     */
    private static LinkedList<String> extractPathFrom(Change change) {
        LinkedList<String> path = new LinkedList<>(Arrays.asList(change
                .getAffectedGlobalId()
                .toString()
                .replace(ROOT_SYMBOL, "")
                .split(PATH_SEPARATOR_SYMBOL)));
        replaceRoot(ROOT_NAME, path);
        return path;
    }

    /**
     * example: from [cloud.foundry.cli.crosscutting.bean.ConfigBean, spec, apps, someApp, manifest]
     *          to [{propertyName}, spec, apps, someApp, manifest]
     */
    private static void replaceRoot(String propertyName, LinkedList<String> path) {
        path.removeFirst();
        path.addFirst(propertyName);
    }
}
