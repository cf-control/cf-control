package cloud.foundry.cli.logic.diff.output;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

//TODO rename to DiffLineBuilder
public class DiffStringBuilder {

    private static final Map<FlagSymbol, WrapperColor> colorMapping;

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

    private DiffStringBuilder() {
        wrapperColor = null;
        flagSymbol = FlagSymbol.NONE;
        indentation = 0;
        propertyName = "";
        value = "";
    }

    public static DiffStringBuilder builder() {
        return new DiffStringBuilder();
    }

    public DiffStringBuilder setIndentation(int indentation) {
        this.indentation = indentation;
        return this;
    }

    public DiffStringBuilder setPropertyName(@Nonnull String propertyName) {
        checkNotNull(propertyName);
        this.propertyName = propertyName;
        return this;
    }

    public DiffStringBuilder setFlagSymbol(@Nonnull FlagSymbol flagSymbol) {
        checkNotNull(flagSymbol);
        this.flagSymbol = flagSymbol;
        return this;
    }

    public DiffStringBuilder setValue(@Nonnull String value) {
        checkNotNull(value);
        this.value = value;
        return this;
    }

    public DiffStringBuilder setWrapperColor(@Nonnull WrapperColor wrapperColor) {
        checkNotNull(wrapperColor);
        this.wrapperColor = wrapperColor;
        return this;
    }

    /**
     * TODO: needs better formatting here
     * {color}{flagSymbol} ' 'x{indentation} + [propertyName: ] + [value] + {colorReset} + {withNewLine ? true => \n}
     * for example:
     *             flagSymbol = FlagSymbol.Added,
     *             indentation = 6,
     *             propertyName = 'size',
     *             value = '1024',
     *             withNewLine = true
     *
     *     Result: "+       size: 1024\n"
     * @return a string as shown above
     */
    public String build() {
        StringBuilder sb = new StringBuilder();

        appendColor(sb);
        sb.append(flagSymbol);
        appendIndentation(sb);
        appendProperty(sb);
        appendValue(sb);
        sb.append(WrapperColor.DEFAULT);

        return sb.toString();
    }

    private void appendProperty(StringBuilder sb) {
        if (!propertyName.isEmpty()) {
            sb.append(propertyName).append(":");

            if (!value.isEmpty()) {
                sb.append(" ");
            }
        }
    }

    private void appendValue(StringBuilder sb) {
        if (!value.isEmpty()) {
            sb.append(value);
        }
    }

    private void appendColor(StringBuilder sb) {
        // if color given use that, else take color from default flag to color mapping
        if (wrapperColor != null) {
            sb.append(wrapperColor);
        } else {
            sb.append(colorMapping.get(flagSymbol));
        }
    }

    private void appendIndentation(StringBuilder sb) {
        for (int i = 0; i < indentation; i++) {
            sb.append(" ");
        }
    }
}
