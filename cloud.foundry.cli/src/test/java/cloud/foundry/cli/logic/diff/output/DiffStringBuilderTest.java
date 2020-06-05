package cloud.foundry.cli.logic.diff.output;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DiffStringBuilderTest {

    @Test
    public void testBuildDefault() {
        // given
        String expected = wrapWithColor(WrapperColor.DEFAULT," ");

        // when
        String diffString = DiffStringBuilder.builder().build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithIndentation() {
        // given
        String expected = wrapWithColor(WrapperColor.DEFAULT,"      ");

        // when
        String diffString = DiffStringBuilder.builder().setIndentation(5).build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithProperty() {
        // given
        String expected = wrapWithColor(WrapperColor.DEFAULT, " property:");

        // when
        String diffString = DiffStringBuilder.builder().setPropertyName("property").build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithValue() {
        // given
        String expected = wrapWithColor(WrapperColor.DEFAULT, " value");

        // when
        String diffString = DiffStringBuilder.builder().setValue("value").build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithFlagSymbolAddedPrependsGreenColor() {
        // given
        String expected = wrapWithColor(WrapperColor.GREEN, "+");

        // when
        String diffString = DiffStringBuilder.builder().setFlagSymbol(FlagSymbol.ADDED).build();

        // then
        assertThat(diffString, is(expected));
    }

     @Test
    public void testBuildWithWrapperColor() {
        // given
        String expected = wrapWithColor(WrapperColor.GREEN, " ");

        // when
        String diffString = DiffStringBuilder.builder().setWrapperColor(WrapperColor.GREEN).build();

        // then
        assertThat(diffString, is(expected));
    }

     @Test
    public void testBuildIndentationWhenInputIsNegativ() {
        // given
        String expected = wrapWithColor(WrapperColor.DEFAULT, " ");

        // when
        String diffString = DiffStringBuilder.builder().setIndentation(-1).build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithAllParameters() {
        // given
        String expected = wrapWithColor(WrapperColor.GREEN, "-      property: value");

        // when
        String diffString = DiffStringBuilder
                .builder()
                .setWrapperColor(WrapperColor.GREEN)
                .setFlagSymbol(FlagSymbol.REMOVED)
                .setIndentation(6)
                .setValue("value")
                .setPropertyName("property")
                .build();

        // then
        assertThat(diffString, is(expected));
    }

    @Test
    public void testBuildWithNullValuesThrowsException() {
        // when
        assertThrows(NullPointerException.class, () -> DiffStringBuilder.builder().setFlagSymbol(null));
        assertThrows(NullPointerException.class, () -> DiffStringBuilder.builder().setValue(null));
        assertThrows(NullPointerException.class, () -> DiffStringBuilder.builder().setPropertyName(null));
        assertThrows(NullPointerException.class, () -> DiffStringBuilder.builder().setWrapperColor(null));
    }

    private String wrapWithColor(WrapperColor wrapperColor, String s) {
        return wrapperColor + s + WrapperColor.DEFAULT;
    }
}
