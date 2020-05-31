package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class YamlPointerTest {

    @Test
    public void testEscapeCharacters() {
        YamlPointer pointer = new YamlPointer("#/this/is~1a/valid/yaml~1~0pointer");

        assertThat(pointer.getNumberOfNodeNames(), is(4));
        assertThat(pointer.getNodeName(0), is("this"));
        assertThat(pointer.getNodeName(1), is("is/a"));
        assertThat(pointer.getNodeName(2), is("valid"));
        assertThat(pointer.getNodeName(3), is("yaml/~pointer"));
    }

    @Test
    public void testDelimiterAtTheEnd() {
        YamlPointer pointer = new YamlPointer("#/delimiterAtTheEnd/");

        assertThat(pointer.getNumberOfNodeNames(), is(1));
        assertThat(pointer.getNodeName(0), is("delimiterAtTheEnd"));
    }

    @Test
    public void testEmptyPointer() {
        YamlPointer pointer = new YamlPointer("#/");

        assertThat(pointer.getNumberOfNodeNames(), is(0));
    }

    @Test
    public void testEmptyPointer2() {
        YamlPointer pointer = new YamlPointer("#");

        assertThat(pointer.getNumberOfNodeNames(), is(0));
    }

    @Test
    public void testInvalidBeginningMissingSlash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("#this/is/invalid"));
    }

    @Test
    public void testInvalidBeginningMissingHash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("/also/this/is/invalid"));
    }

    @Test
    public void testEmptyNodeNameInPointer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("#/this//is/invalid"));
    }

    @Test
    public void testIllegalEscapeSequenceInPointer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("#/this/escape~2sequence/is/invalid"));
    }

    @Test
    public void testNodeIndexOutOfBounds() {
        YamlPointer pointer = new YamlPointer("#/some/pointer");

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> pointer.getNodeName(-1));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> pointer.getNodeName(2));
    }

}
