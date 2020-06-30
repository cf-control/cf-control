package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;


public class YamlMapperTest {

    @Test
    public void dumpTest() {
        //given
        Map<String, ServiceBean> services = new HashMap<>();
        ServiceBean serviceBean = new ServiceBean();
        serviceBean.setService("someService");
        serviceBean.setPlan(null);
        serviceBean.setTags(Arrays.asList("tag1", "tag2"));
        services.put("serviceName", serviceBean);
        //when
        String dump = YamlMapper.dump(services);
        //then
        assertThat(dump, is(
                "serviceName:\n" +
                "  service: someService\n" +
                "  tags:\n" +
                "  - tag1\n" +
                "  - tag2\n"
        ));
    }

    @Test
    public void loadBeanTestWorking() throws IOException {
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
    public void loadBeanTestThrowsIoException() {
        //given
        String configFilePath = "./invalidpath";
        //when + then
        assertThrows(IOException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ApplicationBean.class));
    }

    @Test
    public void loadBeanTestThrowsConstructorException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/Application.yml";
        //when + then
        assertThrows(ConstructorException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ServiceBean.class));
    }

    @Test
    public void loadBeanTestThrowsRefResolvingException() {
        //given
        String configFilePath = "./src/test/resources/refresolver/ApplicationPathWithRefProblem.yml";
        //when + then
        assertThrows(ConstructorException.class, () -> YamlMapper.loadBeanFromFile(configFilePath, ServiceBean.class));
    }
}
