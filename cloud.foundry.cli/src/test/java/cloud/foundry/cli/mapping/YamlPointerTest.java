package cloud.foundry.cli.mapping;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.InvalidPointerException;
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
    public void testInvalidBeginningMissingSlash() {
        assertThrows(
                InvalidPointerException.class,
                () -> new YamlPointer("#this/is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testInvalidBeginningMissingHash() {
        assertThrows(
                InvalidPointerException.class,
                () -> new YamlPointer("/also/this/is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testEntirelyEmptyPointer() {
        assertThrows(
                InvalidPointerException.class,
                () -> new YamlPointer("#/"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testEmptyNodeNameInPointer() {
        assertThrows(
                InvalidPointerException.class,
                () -> new YamlPointer("#/this//is/invalid"));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testIllegalEscapeSequenceInPointer() {
        assertThrows(
                InvalidPointerException.class,
                () -> new YamlPointer("#/this/escape~2sequence/is/invalid"));
    }

}
