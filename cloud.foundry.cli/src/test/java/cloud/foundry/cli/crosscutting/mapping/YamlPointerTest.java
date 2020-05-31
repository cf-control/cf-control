package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

public class YamlPointerTest {

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testGetters() {
        YamlPointer pointer = new YamlPointer("#/this/is~1a/valid/yaml~1~0pointer");

        assertThat(pointer.getNumberOfNodeNames(), is(4));
        assertThat(pointer.getNodeName(0), is("this"));
        assertThat(pointer.getNodeName(1), is("is/a"));
        assertThat(pointer.getNodeName(2), is("valid"));
        assertThat(pointer.getNodeName(3), is("yaml/~pointer"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testEmptyPointer() {
        YamlPointer pointer = new YamlPointer("#/");

        assertThat(pointer.getNumberOfNodeNames(), is(0));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testInvalidBeginningMissingSlash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("#this/is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testInvalidBeginningMissingHash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("/also/this/is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testEmptyNodeNameInPointer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("#/this//is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testIllegalEscapeSequenceInPointer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YamlPointer("#/this/escape~2sequence/is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
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
