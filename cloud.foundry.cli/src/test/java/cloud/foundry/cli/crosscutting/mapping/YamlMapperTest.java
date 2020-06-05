package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.IOException;
import java.util.Arrays;


public class YamlMapperTest {

    @Test
    public void dumpBeanTest() {
        //given
        ServiceBean serviceBean = new ServiceBean();
        serviceBean.setService("serviceName");
        serviceBean.setName("test");
        serviceBean.setApplications(null);
        serviceBean.setPlan("default");
        serviceBean.setId("testId");
        serviceBean.setLastOperation("operation");
        serviceBean.setTags(Arrays.asList("tag1", "tag2"));
        //when
        String dump = YamlMapper.dumpBean(serviceBean);
        //then
        assertThat(dump, is(
                "applications: null\n" +
                "id: testId\n" +
                "lastOperation: operation\n" +
                "name: test\n" +
                "plan: default\n" +
                "service: serviceName\n" +
                "tags:\n" +
                "- tag1\n" +
                "- tag2\n" +
                "type: null\n"
        ));
    }

    @Test
    public void loadBeanTestWorking() throws IOException {
        //given
        String configFilePath = "./src/test/resources/refresolver/Application.yml";
        //when
        ApplicationBean applicationBean = YamlMapper.loadBean(configFilePath, ApplicationBean.class);
        //then
        assertThat(applicationBean.getName(), is("testApp"));
        assertThat(applicationBean.getPath(), is("path/to/specify"));
    }

    @Test
    public void loadBeanTestThrowsIoException() {
        //given
        String configFilePath = "./invalidpath";
        //when + then
        assertThrows(IOException.class, () -> YamlMapper.loadBean(configFilePath, ApplicationBean.class));
    }

    @Test
    public void loadBeanTestThrowsConstructorException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/Application.yml";
        //when + then
        assertThrows(ConstructorException.class, () -> YamlMapper.loadBean(configFilePath, ServiceBean.class));
    }

    @Test
    public void loadBeanTestThrowsRefResolvingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/ApplicationPathWithRefProblem.yml";
        //when + then
        assertThrows(ConstructorException.class, () -> YamlMapper.loadBean(configFilePath, ServiceBean.class));
    }
}