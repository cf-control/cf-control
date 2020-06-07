package cloud.foundry.cli.logic;

import cloud.foundry.cli.crosscutting.exceptions.UnsupportedChangeTypeException;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.util.YamlCreator;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


// TODO: remove this temporary test class, when no longer needed

public class DiffLogicTest {

    //TODO replace this dummy test with actual tests
    @Test
    public void testDiffLogic() throws FileNotFoundException, UnsupportedChangeTypeException {
        Yaml yamlProc = YamlCreator.createDefaultYamlProcessor();
        DiffLogic diffLogic = new DiffLogic();

        ConfigBean configLive = yamlProc.loadAs(new FileInputStream(
            new File("src/test/resources/basic/configLive.yml")), ConfigBean.class);
        ConfigBean configDesired = yamlProc.loadAs(new FileInputStream(
            new File("src/test/resources/basic/configDesired.yml")), ConfigBean.class);

        System.out.println(diffLogic.createDiffOutput(configLive, configDesired));
    }
}
