package cloud.foundry.cli.logic.diff.output;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

//TODO rename to DiffLineBuilder

/**
 * Builder class that let's the user build a diff line with given properties
 *  <ul>
 *      <li>indentation: spaces left to the actual string</li>
 *      <li>propertyName: name of the property (a colon will be added after the property name)</li>
 *      <li>value: value that comes after the property name</li>
 *      <li>flagSymbol: the symbol that is the first character of the diff line ('+', '-', ...)</li>
 *      <li>wrapperColor: color the console should print the line in</li>
 *  </ul>
 *
 * example:
 * <ul>
 *      <li>flagSymbol = FlagSymbol.Added</li>
 *      <li>indentation = 6</li>
 *      <li> propertyName = 'size'</li>
 *      <li>value = '1024'</li>
 *      <li>withNewLine = true</li>
 *      <br>
 *      <li> Result: "[color]+ &nbsp &nbsp &nbsp &nbsp &nbsp &nbsp size: 1024[color.reset]"</li>
 *  </ul>
 */
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

    /**
     *
     * @return the builder object
     */
    public static DiffStringBuilder builder() {
        return new DiffStringBuilder();
    }

    /**
     *
     * @param indentation spaces left to the actual string
     * @return instance of the current builder object
     */
    public DiffStringBuilder setIndentation(int indentation) {
        this.indentation = indentation;
        return this;
    }

    /**
     *
     * @param propertyName name of the property (a colon will be added after the property name)
     * @return instance of the current builder object
     * @throws NullPointerException
     */
    public DiffStringBuilder setPropertyName(@Nonnull String propertyName) {
        checkNotNull(propertyName);
        this.propertyName = propertyName;
        return this;
    }

    /**
     *
     * @param flagSymbol the symbol that is the first character of the diff line ('+', '-', ...)
     * @return instance of the current builder object
     * @throws NullPointerException
     */
    public DiffStringBuilder setFlagSymbol(@Nonnull FlagSymbol flagSymbol) {
        checkNotNull(flagSymbol);
        this.flagSymbol = flagSymbol;
        return this;
    }

    /**
     *
     * @param value value that comes after the property name
     * @return instance of the current builder object
     * @throws NullPointerException
     */
    public DiffStringBuilder setValue(@Nonnull String value) {
        checkNotNull(value);
        this.value = value;
        return this;
    }

    /**
     *
     * @param wrapperColor color the console should print the line in
     * @return instance of the current builder object
     * @throws NullPointerException
     */
    public DiffStringBuilder setWrapperColor(@Nonnull WrapperColor wrapperColor) {
        checkNotNull(wrapperColor);
        this.wrapperColor = wrapperColor;
        return this;
    }

    /**
     * build the diff line with the given parameters of the build process
     * @return the diff line as string
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
