package cloud.foundry.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

public class YamlDemoTest {

    @Test
    public void testLoadLinearSequenceAsList() {
        String yamlDocument = "- one\n" +
                "- two\n" +
                "- three\n";

        Yaml yaml = new Yaml();
        List<String> list = yaml.loadAs(yamlDocument, List.class);

        assertThat(list.size(), is(3));
        assertThat(list.get(0), is("one"));
        assertThat(list.get(1), is("two"));
        assertThat(list.get(2), is("three"));
    }

    @Test
    public void testDumpListAsLinearSequence() {
        LinkedList<String> list = new LinkedList<>();
        list.add("back");
        list.add("front");

        DumperOptions options = new DumperOptions();
        // in order to get list entries across multiple lines:
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);
        String yamlDocument = yaml.dump(list);

        assertThat(yamlDocument, is("- back\n" +
                "- front\n")); // note that the values are sorted alphabetically by default
    }

    public static class Person {

        private String name;
        private int age;

        // to fulfill the JavaBeans specification a class needs:
        // - a public constructor with no arguments
        // - public getters and setters for each attribute

        public Person() { }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void testLoadLinearAssociativeSequenceAsJavaBean() {
        String yamlDocument = "name: John Doe\n" +
                "age: 42";

        Yaml yaml = new Yaml();
        Person person = yaml.loadAs(yamlDocument, Person.class);

        assertThat(person.getName(), is("John Doe"));
        assertThat(person.getAge(), is(42));
    }

    @Test
    public void testDumpSimpleJavaBean() {
        Person person = new Person();
        person.setName("Jane Doe");
        person.setAge(21);

        DumperOptions options = new DumperOptions();
        options.setTags(new HashMap<String, String>()); // do not dump tags into the document

        Yaml yaml = new Yaml(options);
        String yamlDocument = yaml.dumpAsMap(person);

        assertThat(yamlDocument, is("age: 21\n" +
                "name: Jane Doe\n")); // note that the keys are sorted alphabetically by default
    }
}
