package cloud.foundry.cli.logic.diff.output;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DiffLineBuilderTest {

    @Test
    public void testBuildDefault() {
        // given
        String expected = wrapWithColor(AnsiColorCode.DEFAULT," ");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithIndentation() {
        // given
        String expected = wrapWithColor(AnsiColorCode.DEFAULT,"      ");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).setIndentation(5).build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithProperty() {
        // given
        String expected = wrapWithColor(AnsiColorCode.DEFAULT, " property:");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).setPropertyName("property").build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithValue() {
        // given
        String expected = wrapWithColor(AnsiColorCode.DEFAULT, " value");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).setValue("value").build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithFlagSymbolAddedPrependsGreenColor() {
        // given
        String expected = wrapWithColor(AnsiColorCode.GREEN, "+");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).setFlagSymbol(FlagSymbol.ADDED).build();

        // then
        assertThat(diffString, is(expected));
    }

     @Test
    public void testBuildWithAnsiColorCode() {
        // given
        String expected = wrapWithColor(AnsiColorCode.GREEN, " ");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).setColorCode(AnsiColorCode.GREEN).build();

        // then
        assertThat(diffString, is(expected));
    }

     @Test
    public void testBuildIndentationWhenInputIsNegative() {
        // given
        String expected = wrapWithColor(AnsiColorCode.DEFAULT, " ");

        // when
        String diffString = DiffLineBuilder.builder().setColorsEnabled(true).setIndentation(-1).build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithAllParameters() {
        // given
        String expected = wrapWithColor(AnsiColorCode.GREEN, "-      property: value");

        // when
        String diffString = DiffLineBuilder
                .builder()
                .setColorsEnabled(true)
                .setColorCode(AnsiColorCode.GREEN)
                .setFlagSymbol(FlagSymbol.REMOVED)
                .setIndentation(6)
                .setValue("value")
                .setPropertyName("property")
                .build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithoutColorsEnabled() {
        // given
        String expected = "-      property: value";

        // when
        String diffString = DiffLineBuilder
                .builder()
                .setColorCode(AnsiColorCode.GREEN)
                .setFlagSymbol(FlagSymbol.REMOVED)
                .setIndentation(6)
                .setValue("value")
                .setPropertyName("property")
                .build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testNullValuesThrowException() {
        // when
        assertThrows(NullPointerException.class, () -> DiffLineBuilder.builder().setFlagSymbol(null));
        assertThrows(NullPointerException.class, () -> DiffLineBuilder.builder().setValue(null));
        assertThrows(NullPointerException.class, () -> DiffLineBuilder.builder().setPropertyName(null));
        assertThrows(NullPointerException.class, () -> DiffLineBuilder.builder().setColorCode(null));
    }

    private String wrapWithColor(AnsiColorCode colorCode, String s) {
        return colorCode + s + AnsiColorCode.DEFAULT;
    }
}
