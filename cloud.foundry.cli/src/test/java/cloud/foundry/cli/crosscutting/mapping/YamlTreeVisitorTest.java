package cloud.foundry.cli.crosscutting.mapping;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class YamlTreeVisitorTest {

    @Test
    public void testVisitEmptySequence() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        List<Object> sequence = Collections.emptyList();

        YamlTreeVisitor.visit(mockedVisitor, sequence);

        verify(mockedVisitor).visitSequence(sequence);
    }

    @Test
    public void testVisitNonemptySequence() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        List<Object> sequence = Arrays.asList("element", 42, true, 3.14);

        YamlTreeVisitor.visit(mockedVisitor, sequence);

        verify(mockedVisitor).visitSequence(sequence);
    }

    @Test
    public void testVisitEmptyMapping() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        Map<Object, Object> mapping = Collections.emptyMap();

        YamlTreeVisitor.visit(mockedVisitor, mapping);

        verify(mockedVisitor).visitMapping(mapping);
    }

    @Test
    public void testVisitNonemptyMapping() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        Map<Object, Object> mapping = new HashMap<>();
        mapping.put("key", 5);
        mapping.put(false, 2.72);

        YamlTreeVisitor.visit(mockedVisitor, mapping);

        verify(mockedVisitor).visitMapping(mapping);
    }

    @Test
    public void testVisitStringScalar() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        String string = "scalar";

        YamlTreeVisitor.visit(mockedVisitor, string);

        verify(mockedVisitor).visitScalar(string);
    }

    @Test
    public void testVisitIntegerScalar() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);
        Integer integer = 42;

        YamlTreeVisitor.visit(mockedVisitor, integer);

        verify(mockedVisitor).visitScalar(integer);
    }

    @Test
    public void testVisitNullScalar() {
        YamlTreeVisitor mockedVisitor = mock(YamlTreeVisitor.class);

        YamlTreeVisitor.visit(mockedVisitor, null);

        verify(mockedVisitor).visitScalar(null);
    }

}
