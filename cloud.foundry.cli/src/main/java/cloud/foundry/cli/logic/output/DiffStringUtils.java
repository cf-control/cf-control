package cloud.foundry.cli.logic.output;

import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.crosscutting.util.YamlProcessorCreator;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ArrayChange;
import org.javers.core.diff.changetype.container.CollectionChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.map.EntryAdded;
import org.javers.core.diff.changetype.map.EntryRemoved;
import org.javers.core.diff.changetype.map.EntryValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.stream.Collectors;

public class DiffStringUtils {

    /**
     * API method
     * @param propertyName
     * @param indentation
     * @return
     */
    public String fromProperty(FlagSymbol flagSymbol,  int indentation, String propertyName) {
        return asPropertyEntry(flagSymbol, indentation, propertyName);
    }

    /**
     * API method
     * @param flagSymbol
     * @param indentation
     * @param bean
     * @return
     */
    public String fromBean(FlagSymbol flagSymbol, int indentation, Bean bean) {
        Yaml yamlProcessor = YamlProcessorCreator.createNullValueIgnoring();
        String yamlDump = yamlProcessor.dump(bean);

        List<String> yamlLines = List.of(yamlDump.split("\n"));

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

    /**
     * API method
     * @param flagSymbol
     * @param indentation
     * @param bean
     * @param property
     * @return
     */
    public String fromBean(FlagSymbol flagSymbol, int indentation, Bean bean, String property) {
        return asPropertyEntry(flagSymbol, indentation - 2, property) + fromBean(flagSymbol, indentation, bean);
    }

    /**
     * API method
     * @param indentation
     * @param change
     * @return
     */
    public String fromChange(int indentation, Change change) {
        if (change instanceof ContainerChange) {
            return handleContainerChange(indentation, (ContainerChange) change);
        } else if (change instanceof ValueChange) {
            return handleValueChange(indentation, (ValueChange) change);
        } else if (change instanceof MapChange) {
            return handleMapChange(indentation, (MapChange) change) ;
        }

        return "";
    }

    private String handleContainerChange(int indentation, ContainerChange change) {
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
            for (ContainerElementChange element : collectionChange.getChanges()) {

            }
            return sb.toString();
        } else if (change instanceof ArrayChange) {
            //TODO: add ArrayChange handler
        }

        return "";
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
