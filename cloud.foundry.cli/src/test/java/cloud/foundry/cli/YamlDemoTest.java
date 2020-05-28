package cloud.foundry.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

public class YamlDemoTest {

    @Test
    public void testLoadLinearSequenceAsList() {
        String yamlDocument = "- one\n" +
                "- two\n" +
                "- three\n";

        Yaml yaml = new Yaml();
        List<Object> list = yaml.loadAs(yamlDocument, List.class);

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
        private List<BankAccount> bankAccounts;

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

        public List<BankAccount> getBankAccounts() {
            return bankAccounts;
        }

        public void setBankAccounts(List<BankAccount> bankAccounts) {
            this.bankAccounts = bankAccounts;
        }
    }

    public static class BankAccount {

        private String accountNumber;
        private String bankCode;

        public BankAccount(){}

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getBankCode() {
            return bankCode;
        }

        public void setBankCode(String bankCode) {
            this.bankCode = bankCode;
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
                                        "bankAccounts: null\n" +
                                        "name: Jane Doe\n"));
        // note that the keys are sorted alphabetically by default
    }

    @Test
    public void testLoadNestedSequenceAsJavaBean() {
        String yamlDocument =   "age: 42\n" +
                                "bankAccounts:\n" +
                                "- accountNumber: '0009131958'\n" +
                                "  bankCode: '57210000'\n" +
                                "- accountNumber: '0009171113'\n" +
                                "  bankCode: '57210000'\n" +
                                "name: John Doe\n";

        Yaml yaml = new Yaml();
        Person person = yaml.loadAs(yamlDocument, Person.class);

        assertThat(person.getName(), is("John Doe"));
        assertThat(person.getAge(), is(42));
        assertThat(person.getBankAccounts().size(), is(2));
        assertThat(person.getBankAccounts().get(0).getAccountNumber(), is("0009131958"));
        assertThat(person.getBankAccounts().get(0).getBankCode(), is("57210000"));
        assertThat(person.getBankAccounts().get(1).getAccountNumber(), is("0009171113"));
        assertThat(person.getBankAccounts().get(1).getBankCode(), is("57210000"));
    }

    @Test
    public void testDumpNestedJavaBean() {
        Person person = new Person();
        person.setName("Jane Doe");
        person.setAge(21);

        BankAccount bankAccount1 = new BankAccount();
        bankAccount1.setAccountNumber("0009131958");
        bankAccount1.setBankCode("57210000");

        BankAccount bankAccount2 = new BankAccount();
        bankAccount2.setAccountNumber("0009171113");
        bankAccount2.setBankCode("57210000");

        person.setBankAccounts(Arrays.asList(bankAccount1, bankAccount2));

        // do not dump tags into the document
        DumperOptions options = new DumperOptions();
        options.setTags(new HashMap<>());

        Yaml yaml = new Yaml(options);
        String yamlDocument = yaml.dumpAsMap(person);

        // note that the keys are sorted alphabetically by default
        assertThat(yamlDocument, is("age: 21\n" +
                                        "bankAccounts:\n" +
                                        "- accountNumber: '0009131958'\n" +
                                        "  bankCode: '57210000'\n" +
                                        "- accountNumber: '0009171113'\n" +
                                        "  bankCode: '57210000'\n" +
                                        "name: Jane Doe\n"));
    }
}
