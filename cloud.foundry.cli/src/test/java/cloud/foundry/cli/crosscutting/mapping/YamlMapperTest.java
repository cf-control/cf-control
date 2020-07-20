package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.YamlParsingException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;


public class YamlMapperTest {

    private static final String SERVICE_YAML_EXAMPLE =
                    "service: someService\n" +
                    "tags:\n" +
                    "- tag1\n" +
                    "- tag2\n";

    @Test
    public void testDump() {
        // given
        ServiceBean serviceBean = new ServiceBean();

        serviceBean.setService("someService");
        serviceBean.setPlan(null);
        serviceBean.setTags(Arrays.asList("tag1", "tag2"));

        // when
        String dump = YamlMapper.dump(serviceBean);

        // then
        assertThat(dump, is(SERVICE_YAML_EXAMPLE));
    }

    @Test
    public void testResolveYamlFile() throws IOException {
        //given
        String yamlFilePath = "./src/test/resources/refresolver/RemoteRef.yaml";

        //when
        String resolvedYamlFileContent = YamlMapper.resolveYamlFile(yamlFilePath);

        //then
        assertThat(resolvedYamlFileContent, is(
                "me:\n" +
                "  name: John\n" +
                "  age: 42\n" +
                "myFriend:\n" +
                "  name: Jane\n" +
                "  age: 37\n"));
    }

    @Test
    public void testResolveYamlFileThrowsRefResolvingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/ApplicationWithRefProblem.yml";
        //when + then
        assertThrows(RefResolvingException.class, () -> YamlMapper.resolveYamlFile(configFilePath));
    }

    @Test
    public void testInterpretBean() {
        // when
        ServiceBean interpretedBean = YamlMapper.interpretBean(SERVICE_YAML_EXAMPLE, ServiceBean.class);

        // then
        assertThat(interpretedBean.getService(), is("someService"));
        assertThat(interpretedBean.getPlan(), is(nullValue()));
        assertThat(interpretedBean.getTags().size(), is(2));
        assertThat(interpretedBean.getTags().get(0), is("tag1"));
        assertThat(interpretedBean.getTags().get(1), is("tag2"));
    }

    @Test
    public void testInterpretBeanThrowsYamlParsingException() {
        // given
        String serviceYaml = SERVICE_YAML_EXAMPLE + "nonExistentProperty: someValue\n";

        // when + then
        assertThrows(YamlParsingException.class, () -> YamlMapper.interpretBean(serviceYaml, ServiceBean.class));
    }

    @Test
    public void testInterpretBeanThrowsIllegalArgumentException() {
        // given
        String serviceYaml = SERVICE_YAML_EXAMPLE + "property : value : uninterpretable\n";

        // when + then
        assertThrows(IllegalArgumentException.class, () -> YamlMapper.interpretBean(serviceYaml, ServiceBean.class));
    }

    @Test
    public void testLoadBean() throws IOException {
        //given
        String configFilePath = "./src/test/resources/refresolver/Application.yml";
        //when
        SpecBean specBean = YamlMapper.loadBeanFromFile(configFilePath, SpecBean.class);
        //then
        assertThat(specBean.getSpaceDevelopers(), is(nullValue()));
        assertThat(specBean.getServices(), is(nullValue()));

        Map<String, ApplicationBean> applicationBeans = specBean.getApps();
        assertThat(applicationBeans.size(), is(1));
        Entry<String, ApplicationBean> applicationEntry = applicationBeans.entrySet().iterator().next();

        String applicationName = applicationEntry.getKey();
        assertThat(applicationName, is("testApp"));

        ApplicationBean applicationBean = applicationEntry.getValue();
        assertThat(applicationBean.getPath(), is("path/to/specify"));
    }

    @Test
    public void testLoadBeanThrowsIoException() {
        //given
        String configFilePath = "./invalidpath";

        //when + then
        assertThrows(IOException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ApplicationBean.class));
    }

    @Test
    public void testLoadBeanThrowsYamlParsingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/Application.yml";

        //when + then
        assertThrows(YamlParsingException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ServiceBean.class));
    }

    @Test
    public void testLoadBeanThrowsRefResolvingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/ApplicationWithRefProblem.yml";

        //when + then
        assertThrows(RefResolvingException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ServiceBean.class));
    }

    @Test
    public void testLoadBeanOnSyntaxProblemThrowsYamlParsingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/YamlSyntaxProblem.yml";

        //when + then
        assertThrows(YamlParsingException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ServiceBean.class));
    }

    @Test
    public void testLoadBeanOnInterpretationProblemThrowsYamlParsingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/YamlInterpretationProblem.yml";

        //when + then
        assertThrows(YamlParsingException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ServiceBean.class));
    }
}
