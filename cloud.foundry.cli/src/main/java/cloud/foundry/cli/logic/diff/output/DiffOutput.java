package cloud.foundry.cli.logic.diff.output;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.logic.diff.DiffNode;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.ChangeType;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerValueChanged;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class transforms a difference tree into a visual representation of configuration differences.
 */
public class DiffOutput {

    private static final int DEFAULT_INDENTATION_INCREMENT = 2;

    private final int indentationIncrement;

    /**
     * Constructor with default indentation increment.
     */
    public DiffOutput() {
        this.indentationIncrement = DEFAULT_INDENTATION_INCREMENT;
    }

    /**
     * Construct with a custom indentation interval.
     *
     * @param indentationIncrement indentation interval (>= 1)
     * @throws IllegalArgumentException in case an invalid indentation interval is passed
     */
    public DiffOutput(int indentationIncrement) {
        checkArgument(indentationIncrement >= 1, "YAML file output must be indented, values < 1 are not allowed");

        this.indentationIncrement = indentationIncrement;
    }

    /**
     * Transforms a difference tree into a visual representation of configuration differences.
     * @param node root of the difference tree that should be parse to the difference output
     * @return string of the difference output
     * @throws NullPointerException if the argument is null
     */
    public String from(DiffNode node) {
        checkNotNull(node);

        List<String> lines = new LinkedList<>();
        diffLines(lines, node);
        return String.join("\n", lines);
    }

    private int calculateIndentationFromDepth(int depth) {
        return depth * indentationIncrement;
    }

    private void diffLines(List<String> lines, DiffNode node) {
        List<CfChange> changes = node.getChanges();

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
                lines.add(fromProperty(FlagSymbol.NONE, propertyIndentation, node.getPropertyName()));
            }

            for (CfChange change : changes) {
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
        String yamlDump = YamlMapper.dump(bean);

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
        } else if (change instanceof CfObjectValueChanged) {
            return handleValueChange(indentation, (CfObjectValueChanged) change);
        } else {
            return handleMapChange(indentation, (CfMapChange) change) ;
        }
    }

    private List<String> handleContainerChange(int indentation, CfContainerChange change) {
        List<String> lines = new LinkedList<>();
        lines.add(asPropertyEntry(FlagSymbol.NONE, indentation, change.getPropertyName()));

        for (CfContainerValueChanged element : change.getChangedValues()) {
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

    private List<String> handleValueChange(int indentation, CfObjectValueChanged valueChange) {
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

        for (CfMapValueChanged element : change.getChangedValues()) {
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
