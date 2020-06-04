package cloud.foundry.cli.logic.diff.output;

import cloud.foundry.cli.crosscutting.exceptions.NotSupportedChangeType;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.logic.diff.DiffNode;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.CollectionChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * this class transforms a difference tree into a visual representation of configuration differences
 */
public class DiffOutput {

    private static final int DEFAULT_INDENTATION = 2;

    private int indentation;

    /**
     * sets the indentation of the output to the given value
     * @param indentation value for number of spaces to the left of the output
     */
    public void setIndentation(int indentation) {
        this.indentation = indentation;
    }

    public DiffOutput() {
        this.indentation = DEFAULT_INDENTATION;
    }

    /**
     * transforms a difference tree into a visual representation of configuration differences
     * @param node root of the difference tree that should be parse to the difference output
     * @return string of the difference output
     * @throws NotSupportedChangeType when a change type was used that is not supported within our bean hierarchy
     */
    public String from(@Nonnull DiffNode node) throws NotSupportedChangeType {
        return this.toDiffString(node, this.indentation);
    }

    //TODO pass StringBuilder with parameters to be more efficient
    private String toDiffString(DiffNode node, int indentation) throws NotSupportedChangeType {
        List<Change> changes = node.getChanges();

        //no changes at this level which means, there are no changes at the sub-levels also
        if (changes.size() == 0 && node.isLeaf()) return "";

        if (node.isNewObject()) {
            return fromBean(FlagSymbol.ADDED,
                    indentation,
                    (Bean) changes.get(0).getAffectedObject().get(),
                    node.getPropertyName());
        } else if (node.isRemovedObject()) {
            return fromBean(FlagSymbol.REMOVED,
                    indentation,
                    (Bean) changes.get(0).getAffectedObject().get(),
                    node.getPropertyName());
        } else {

            StringBuilder sb = new StringBuilder();
            //TODO remove magic value
            // calculate current indentation in relation to current node depth
            sb.append(fromProperty(FlagSymbol.NONE, indentation - 2,  node.getPropertyName()));

            for (Change change : changes) {
                if (change instanceof MapChange && ! node.isLeaf()) continue;

                sb.append(fromChange(indentation, change));
            }

            // recursion
            for (DiffNode childNode : node.getChildNodes().values()) {
                sb.append(toDiffString(childNode, 2 + indentation));
            }
            return sb.toString();
        }
    }

    private String fromProperty(FlagSymbol flagSymbol, int indentation, String propertyName) {
        return asPropertyEntry(flagSymbol, indentation, propertyName);
    }

    private String fromBean(FlagSymbol flagSymbol, int indentation, Bean bean) {
        Yaml yamlProcessor = YamlCreator.createDefaultYamlProcessor();
        String yamlDump = yamlProcessor.dump(bean);

        List<String> yamlLines = Arrays.asList(yamlDump.split("\n"));

        // to each line in the yaml dump
        return yamlLines
                .stream()
                // apply flag and indentation
                .map(line -> DiffStringBuilder.builder()
                        .setFlagSymbol(flagSymbol)
                        .setIndentation(indentation)
                        .setValue(line)
                        .build())
                .collect(Collectors.joining("\n"))
                + "\n";
    }

    private String fromBean(FlagSymbol flagSymbol, int indentation, Bean bean, String property) {
        return asPropertyEntry(flagSymbol, indentation - 2, property) + fromBean(flagSymbol, indentation, bean);
    }

    private String fromChange(int indentation, Change change) throws NotSupportedChangeType {
        if (change instanceof ContainerChange) {
            return handleContainerChange(indentation, (ContainerChange) change);
        } else if (change instanceof ValueChange) {
            return handleValueChange(indentation, (ValueChange) change);
        } else if (change instanceof MapChange) {
            return handleMapChange(indentation, (MapChange) change) ;
        }
        // can be a reference change, but that's not relevant to use, so just skip
        return "";
    }

    private String handleContainerChange(int indentation, ContainerChange change) throws NotSupportedChangeType {
        if (change instanceof CollectionChange) {
            CollectionChange collectionChange = (CollectionChange) change;

            StringBuilder sb = new StringBuilder();
            sb.append(asPropertyEntry(FlagSymbol.NONE, indentation, change.getPropertyName()));

            for (Object element : collectionChange.getAddedValues()) {
                sb.append(asListEntry(FlagSymbol.ADDED,
                        indentation,
                        element.toString()));
            }
            for (Object element : collectionChange.getRemovedValues()) {
                sb.append(asListEntry(FlagSymbol.REMOVED,
                        indentation,
                        element.toString()));
            }
            //TODO check edge cases
            //TODO research if getChanges() only captures ordering changes or other type of changes also
            for (ContainerElementChange element : collectionChange.getChanges()) {

            }

            return sb.toString();
        }

        throw new NotSupportedChangeType("Change of type " + change.getClass() + " not supported");
    }

    private String handleValueChange(int indentation, ValueChange valueChange) {
        StringBuilder sb = new StringBuilder();

        if (valueChange.getRight() != null) {
            sb.append(asAddedKeyValueEntry(indentation,
                    valueChange.getPropertyName(),
                    valueChange.getRight().toString()));
        }
         if (valueChange.getLeft() != null) {
            sb.append(asRemovedKeyValueEntry(indentation,
                  valueChange.getPropertyName(),
                  valueChange.getLeft().toString()));
        }

        return sb.toString();
    }


    private String handleMapChange(int indentation, MapChange change) {
        StringBuilder sb = new StringBuilder();
        sb.append(asPropertyEntry(FlagSymbol.NONE, indentation, change.getPropertyName()));

        for (EntryAdded element : change.getEntryAddedChanges()) {
            sb.append(asKeyValueEntry(FlagSymbol.ADDED,
                    indentation,
                    "",
                    element.toString()));
        }
        for (EntryRemoved element :  change.getEntryRemovedChanges()) {
            sb.append(asKeyValueEntry(FlagSymbol.REMOVED,
                    indentation,
                    "",
                    element.toString()));
        }
        //TODO check edge cases
        //TODO research if getChanges() only captures ordering changes or other type of changes also
        for (EntryValueChange element :  change.getEntryValueChanges()) {

        }
        return sb.toString();
    }


    /**
     * example : asAddedKeyValueEntry(4, 'name', 'elephantsql') :== '+    name: elephantsql'
     */
    private String asAddedKeyValueEntry(int indentation, String propertyName, String value) {
        return asKeyValueEntry(FlagSymbol.ADDED, indentation, propertyName, value);
    }

    /**
     * example : asRemovedKeyValueEntry(4, 'name', 'elephantsql') :== '-    name: elephantsql'
     */
    private String asRemovedKeyValueEntry(int indentation, String propertyName, String value) {
        return asKeyValueEntry(FlagSymbol.REMOVED, indentation, propertyName, value);
    }

    /**
     * example : asListEntry('+', 4, 'elephantsql') :== '+    - elephantsql'
     */
    private String asListEntry(FlagSymbol flagSymbol, int indentation, String value) {
        return asKeyValueEntry(flagSymbol, indentation, "", "- " + value);
    }

    /**
     * example : asPropertyEntry('+', 4, 'manifest') :== '+    manifest:'
     */
    private String asPropertyEntry(FlagSymbol flagSymbol, int indentation, String property) {
        return asKeyValueEntry(flagSymbol, indentation, property, "");
    }

    /**
     * example : asKeyValueEntry('+', 4, 'diskQuota', '1024') :== '+    diskQuota: 1024'
     */
    private String asKeyValueEntry(FlagSymbol flagSymbol, int indentation, String property, String value) {
        return DiffStringBuilder.builder()
                .setFlagSymbol(flagSymbol)
                .setIndentation(indentation)
                .setPropertyName(property)
                .setValue(value)
                .setNewLine(true)
                .build();
    }

}
