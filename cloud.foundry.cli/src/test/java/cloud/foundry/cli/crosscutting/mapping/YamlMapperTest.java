package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class YamlMapperTest {

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

}