package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.DiffException;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import org.junit.jupiter.api.Test;

import java.io.IOException;


// TODO: remove this temporary test class, when no longer needed

public class DiffLogicTest {

    //TODO replace this dummy test with actual tests
    @Test
    public void testDiffLogic() throws IOException, DiffException {
        DiffLogic diffLogic = new DiffLogic();

        ConfigBean configLive = YamlMapper.loadBean("src/test/resources/basic/configLive.yml", ConfigBean.class);
        ConfigBean configDesired = YamlMapper.loadBean("src/test/resources/basic/configDesired.yml", ConfigBean.class);

        System.out.println(diffLogic.createDiffOutput(configLive, configDesired));
    }
}
