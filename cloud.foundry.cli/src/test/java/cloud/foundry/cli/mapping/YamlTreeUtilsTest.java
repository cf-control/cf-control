package cloud.foundry.cli.mapping;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class YamlTreeUtilsTest {

    //TODO tests for getDescendantNode

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitEmptySequence() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        List<Object> sequence = Collections.emptyList();

        YamlTreeUtils.visit(mockedVisitor, sequence);

        verify(mockedVisitor).visitSequence(sequence);
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitNonemptySequence() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        List<Object> sequence = Arrays.asList("element", 42, true, 3.14);

        YamlTreeUtils.visit(mockedVisitor, sequence);

        verify(mockedVisitor).visitSequence(sequence);
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitEmptyMapping() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        Map<Object, Object> mapping = Collections.emptyMap();

        YamlTreeUtils.visit(mockedVisitor, mapping);

        verify(mockedVisitor).visitMapping(mapping);
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitNonemptyMapping() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        Map<Object, Object> mapping = Map.of("key", 5, false, 2.72);

        YamlTreeUtils.visit(mockedVisitor, mapping);

        verify(mockedVisitor).visitMapping(mapping);
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitStringScalar() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        String string = "scalar";

        YamlTreeUtils.visit(mockedVisitor, string);

        verify(mockedVisitor).visitScalar(string);
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitIntegerScalar() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        Integer integer = 42;

        YamlTreeUtils.visit(mockedVisitor, integer);

        verify(mockedVisitor).visitScalar(integer);
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void testVisitNullScalar() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);

        YamlTreeUtils.visit(mockedVisitor, null);

        verify(mockedVisitor).visitScalar(null);
    }

}
