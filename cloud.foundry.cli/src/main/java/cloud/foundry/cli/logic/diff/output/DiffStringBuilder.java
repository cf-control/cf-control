package cloud.foundry.cli.logic.diff.output;

import java.util.HashMap;
import java.util.Map;

public class DiffStringBuilder {

    private static Map<FlagSymbol, WrapperColor> colorMapping;

    static {
        colorMapping = new HashMap<>();
        colorMapping.put(cloud.foundry.cli.logic.diff.output.FlagSymbol.ADDED, cloud.foundry.cli.logic.diff.output.WrapperColor.GREEN);
        colorMapping.put(cloud.foundry.cli.logic.diff.output.FlagSymbol.REMOVED, cloud.foundry.cli.logic.diff.output.WrapperColor.RED);
        colorMapping.put(cloud.foundry.cli.logic.diff.output.FlagSymbol.NONE, cloud.foundry.cli.logic.diff.output.WrapperColor.DEFAULT);
    }

    private int indentation;
    private String propertyName;
    private cloud.foundry.cli.logic.diff.output.FlagSymbol flagSymbol;
    private cloud.foundry.cli.logic.diff.output.WrapperColor wrapperColor;
    private String value;
    private boolean withNewLine;

    private DiffStringBuilder() {
        wrapperColor = null;
        flagSymbol = cloud.foundry.cli.logic.diff.output.FlagSymbol.NONE;
        indentation = 0;
        propertyName = "";
        value = "";
        withNewLine = false;
    }

    public static DiffStringBuilder builder() {
        return new DiffStringBuilder();
    }

    public DiffStringBuilder setIndentation(int indentation) {
        this.indentation = indentation;
        return this;
    }

    public DiffStringBuilder setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public DiffStringBuilder setFlagSymbol(cloud.foundry.cli.logic.diff.output.FlagSymbol flagSymbol) {
        this.flagSymbol = flagSymbol;
        return this;
    }

    public DiffStringBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public DiffStringBuilder setWrapperColor(cloud.foundry.cli.logic.diff.output.WrapperColor wrapperColor) {
        this.wrapperColor = wrapperColor;
        return this;
    }

    public DiffStringBuilder setNewLine(boolean withNewLine) {
        this.withNewLine = withNewLine;
        return this;
    }

    /**
     * {color}{flagSymbol} ' 'x{indentation} + [propertyName: ] + [value] + {colorReset} + {withNewLine ? true => \n}
     * for example:
     *                 flagSymbol = FlagSymbol.Added,
     *                 indentation = 6,
     *                 propertyName = 'size',
     *                 value = '1024',
     *                 withNewLine = true
     *
     *     Result: "+       size: 1024\n"
     */
    public String build() {
        StringBuilder sb = new StringBuilder();

        if (wrapperColor != null) {
            sb.append(wrapperColor);
        } else {
            sb.append(colorMapping.get(flagSymbol));
        }

        sb.append(flagSymbol);

        repeat(sb, ' ', indentation);

        if (!propertyName.isEmpty()) {
            sb.append(propertyName).append(": ");
        }
        if (!value.isEmpty()) {
            sb.append(value);
        }
        sb.append(cloud.foundry.cli.logic.diff.output.WrapperColor.DEFAULT);

        if (withNewLine) {
            sb.append("\n");
        }

        return sb.toString();
    }

    private void repeat(StringBuilder sb, char c, int times) {
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
    }
}
