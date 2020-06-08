package cloud.foundry.cli.logic.diff.output;

import static cloud.foundry.cli.crosscutting.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

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
public class DiffLineBuilder {

    private static final Map<FlagSymbol, AnsiColorCode> colorMap;

    static {
        colorMap = new HashMap<>();
        colorMap.put(FlagSymbol.ADDED, AnsiColorCode.GREEN);
        colorMap.put(FlagSymbol.REMOVED, AnsiColorCode.RED);
        colorMap.put(FlagSymbol.NONE, AnsiColorCode.DEFAULT);
    }

    private int indentation;
    private String propertyName;
    private FlagSymbol flagSymbol;
    private AnsiColorCode colorCode;
    private String value;

    private boolean colorsEnabled;

    private DiffLineBuilder() {
        colorCode = null;
        flagSymbol = FlagSymbol.NONE;
        indentation = 0;
        propertyName = "";
        value = "";

        // by default, we don't want to insert any color escape sequences in case the I/O we talk to is not a tty
        // https://stackoverflow.com/a/1403817
        // this can be overwritten by the user, though, by calling the corresponding setter
        colorsEnabled = (System.console() != null || System.getenv("COLORS") != null);
    }

    /**
     *
     * @return the builder object
     */
    public static DiffLineBuilder builder() {
        return new DiffLineBuilder();
    }

    /**
     * Enable or disable color output. By default, colors are only enabled automatically when running the tool from a
     * console. This method allows for overwriting that setting.
     * @param enabled whether to enable or disable color outputs
     * @return current builder object
     */
    public DiffLineBuilder setColorsEnabled(boolean enabled) {
        this.colorsEnabled = enabled;
        return this;
    }

    /**
     *
     * @param indentation spaces left to the actual string
     * @return instance of the current builder object
     */
    public DiffLineBuilder setIndentation(int indentation) {
        this.indentation = indentation;
        return this;
    }

    /**
     *
     * @param propertyName name of the property (a colon will be added after the property name)
     * @return instance of the current builder object
     * @throws NullPointerException
     */
    public DiffLineBuilder setPropertyName(@Nonnull String propertyName) {
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
    public DiffLineBuilder setFlagSymbol(@Nonnull FlagSymbol flagSymbol) {
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
    public DiffLineBuilder setValue(@Nonnull String value) {
        checkNotNull(value);
        this.value = value;
        return this;
    }

    /**
     *
     * @param colorCode color the console should print the line in
     * @return instance of the current builder object
     * @throws NullPointerException
     */
    public DiffLineBuilder setColorCode(@Nonnull AnsiColorCode colorCode) {
        checkNotNull(colorCode);
        this.colorCode = colorCode;
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
        appendDefaultColor(sb);

        return sb.toString();
    }

    private void appendColor(StringBuilder sb) {
        // we don't want to insert any color escape sequences in case the I/O we talk to is not a tty
        // https://stackoverflow.com/a/1403817
        if (colorsEnabled) {
            // if color given, use that, else take color from default flag-to-color mapping
            if (colorCode != null) {
                sb.append(colorCode);
            } else {
                sb.append(colorMap.get(flagSymbol));
            }
        }
    }

    private void appendIndentation(StringBuilder sb) {
        for (int i = 0; i < indentation; i++) {
            sb.append(" ");
        }
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

    private void appendDefaultColor(StringBuilder sb) {
        if (colorsEnabled) {
            sb.append(AnsiColorCode.DEFAULT);
        }
    }

}
