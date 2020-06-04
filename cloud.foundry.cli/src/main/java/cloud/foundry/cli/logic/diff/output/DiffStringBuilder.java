package cloud.foundry.cli.logic.diff.output;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class DiffStringBuilder {

    private static Map<FlagSymbol, WrapperColor> colorMapping;

    static {
        colorMapping = new HashMap<>();
        colorMapping.put(FlagSymbol.ADDED, WrapperColor.GREEN);
        colorMapping.put(FlagSymbol.REMOVED, WrapperColor.RED);
        colorMapping.put(FlagSymbol.NONE, WrapperColor.DEFAULT);
    }

    private int indentation;
    private String propertyName;
    private FlagSymbol flagSymbol;
    private WrapperColor wrapperColor;
    private String value;
    private boolean withNewLine;

    private DiffStringBuilder() {
        wrapperColor = null;
        flagSymbol = FlagSymbol.NONE;
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

    public DiffStringBuilder setPropertyName(@Nonnull String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public DiffStringBuilder setFlagSymbol(@Nonnull FlagSymbol flagSymbol) {
        this.flagSymbol = flagSymbol;
        return this;
    }

    public DiffStringBuilder setValue(@Nonnull String value) {
        this.value = value;
        return this;
    }

    public DiffStringBuilder setWrapperColor(@Nonnull WrapperColor wrapperColor) {
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
        sb.append(WrapperColor.DEFAULT);

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
