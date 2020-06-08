package cloud.foundry.cli.logic.diff.output;

import cloud.foundry.cli.crosscutting.exceptions.UnsupportedChangeTypeException;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChangeValue;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChangeValue;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChange;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * this class transforms a difference tree into a visual representation of configuration differences
 */
public class DiffOutput {

    private static final int DEFAULT_INDENTATION_INCREMENT = 2;

    private final int indentationIncrement;

    /**
     * Default constructor
     */
    public DiffOutput() {
        this.indentationIncrement = DEFAULT_INDENTATION_INCREMENT;
    }

    /**
     * Construct with a custom indentation interval
     *
     * @param indentationIncrement indentation interval (>= 1)
     * @throws IllegalArgumentException in case an invalid indentation interval is passed
     */
    public DiffOutput(int indentationIncrement) {
        if (indentationIncrement >= 1) {
            throw new IllegalArgumentException("YAML file output must be indented, values < 1 are not allowed");
        }

        this.indentationIncrement = indentationIncrement;
    }

    /**
     * transforms a difference tree into a visual representation of configuration differences
     * @param node root of the difference tree that should be parse to the difference output
     * @return string of the difference output
     * @throws UnsupportedChangeTypeException when a change type was used that is not supported within our bean
     *  hierarchy
     */
    public String from(@Nonnull DiffNode node) {
        List<String> lines = new LinkedList<>();
        diffLines(lines, node);
        return String.join("\n", lines);
    }
    
    private int calculateIndentationFromDepth(int depth) {
        return depth * indentationIncrement;
    }

    private void diffLines(List<String> lines, DiffNode node) {
        List<CfChange> changes = node.getChanges();

        // no changes at this level which means, there are no changes at the sub-levels also
        if (changes.size() == 0 && node.isLeaf()) {
            return;
        }

        final int depth = node.getDepth();
        final int thisNodeIndentation = calculateIndentationFromDepth(depth);
        final int propertyIndentation = calculateIndentationFromDepth(depth - 1);

        if (node.isNewObject()) {
            lines.addAll(fromBean(FlagSymbol.ADDED,
                    thisNodeIndentation,
                    (Bean) changes.get(0).getAffectedObject(),
                    node.getPropertyName())
            );
        } else if (node.isRemovedObject()) {
            lines.addAll(fromBean(FlagSymbol.REMOVED,
                    thisNodeIndentation,
                    (Bean) changes.get(0).getAffectedObject(),
                    node.getPropertyName())
            );
        } else {
            // we dont want to print the root property since it's not part our yaml config specification
            if (!node.isRoot()) {
                //TODO remove magic value
                // calculate current indentation in relation to current node depth
                lines.add(fromProperty(FlagSymbol.NONE, propertyIndentation, node.getPropertyName()));
            }

            for (CfChange change : changes) {
                // TODO this line is necessary else changes will be displayed that are not relevant to the diff output
                // TODO maybe already remove such changes when creating the diff tree
                if (change instanceof CfMapChange && ! node.isLeaf()) continue;

                lines.addAll(fromChange(thisNodeIndentation, change));
            }

            // recursion
            for (DiffNode childNode : node.getChildNodes()) {
                diffLines(lines, childNode);
            }
        }
    }

    private String fromProperty(FlagSymbol flagSymbol, int indentation, String propertyName) {
        return asPropertyEntry(flagSymbol, indentation, propertyName);
    }

    private List<String> fromBean(FlagSymbol flagSymbol, int indentation, Bean bean) {
        Yaml yamlProcessor = YamlCreator.createDefaultYamlProcessor();
        String yamlDump = yamlProcessor.dump(bean);

        List<String> yamlLines = Arrays.asList(yamlDump.split("\n"));

        // to each line in the yaml dump
        return yamlLines
                .stream()
                // apply flag and indentation
                .map(line -> DiffLineBuilder.builder()
                        .setFlagSymbol(flagSymbol)
                        .setIndentation(indentation)
                        .setValue(line)
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> fromBean(FlagSymbol flagSymbol, int indentation, Bean bean, String property) {
        List<String> lines = new LinkedList<>();
        lines.add(asPropertyEntry(flagSymbol, indentation - indentationIncrement, property));
        lines.addAll(fromBean(flagSymbol, indentation, bean));
        return lines;
    }

    private List<String> fromChange(int indentation, CfChange change) {
        if (change instanceof CfContainerChange) {
            return handleContainerChange(indentation, (CfContainerChange) change);
        } else if (change instanceof CfObjectValueChange) {
            return handleValueChange(indentation, (CfObjectValueChange) change);
        } else {
            return handleMapChange(indentation, (CfMapChange) change) ;
        }
    }

    private List<String> handleContainerChange(int indentation, CfContainerChange change) {
        List<String> lines = new LinkedList<>();
        lines.add(asPropertyEntry(FlagSymbol.NONE, indentation, change.getPropertyName()));

        for (CfContainerChangeValue element : change.getChangedValues()) {
            if (element.getChangeType() == ChangeType.ADDED) {
                lines.add(asListEntry(FlagSymbol.ADDED,
                        indentation,
                        element.getValue()));
            } else if (element.getChangeType() == ChangeType.REMOVED) {
                lines.add(asListEntry(FlagSymbol.REMOVED,
                        indentation,
                        element.getValue()));
            }
        }
        return lines;
    }

    private List<String> handleValueChange(int indentation, CfObjectValueChange valueChange) {
        List<String> lines = new LinkedList<>();

        if (!valueChange.getValueAfter().isEmpty()) {
            lines.add(asAddedKeyValueEntry(indentation,
                    valueChange.getPropertyName(),
                    valueChange.getValueAfter()));
        }
         if (!valueChange.getValueBefore().isEmpty()) {
             lines.add(asRemovedKeyValueEntry(indentation,
                  valueChange.getPropertyName(),
                  valueChange.getValueBefore()));
        }

        return lines;
    }


    private List<String> handleMapChange(int indentation, CfMapChange change) {
        List<String> lines = new LinkedList<>();
        lines.add(asPropertyEntry(FlagSymbol.NONE, indentation, change.getPropertyName()));

        int valueIndentation = indentation + indentationIncrement;

        for (CfMapChangeValue element : change.getChangedValues()) {
            if (element.getChangeType() == ChangeType.ADDED) {
                lines.add(asKeyValueEntry(FlagSymbol.ADDED,
                        valueIndentation,
                        element.getKey(),
                        element.getValueAfter()));
            } else if ( element.getChangeType() == ChangeType.REMOVED) {
                lines.add(asKeyValueEntry(FlagSymbol.REMOVED,
                        valueIndentation,
                        element.getKey(),
                        element.getValueBefore()));
            } else {
                lines.add(asAddedKeyValueEntry(valueIndentation, element.getKey(), element.getValueAfter()));
                lines.add(asRemovedKeyValueEntry(valueIndentation, element.getKey(), element.getValueBefore()));
            }
        }
        return lines;
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
        return DiffLineBuilder.builder()
                .setFlagSymbol(flagSymbol)
                .setIndentation(indentation)
                .setPropertyName(property)
                .setValue(value)
                .build();
    }

}
