package cloud.foundry.cli;

import cloud.foundry.cli.crosscutting.exceptions.NotSupportedChangeType;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.util.YamlProcessorCreator;
import cloud.foundry.cli.logic.DiffLogic;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class JaversTest {

    @Test
    public void testJaversSimple() throws FileNotFoundException, NotSupportedChangeType {
        Yaml yamlProc = YamlProcessorCreator.createNullValueIgnoring();
        DiffLogic diffLogic = new DiffLogic();

        ConfigBean configLive = yamlProc.loadAs(new FileInputStream(
            new File("src/test/resources/basic/configLive.yml")), ConfigBean.class);
        ConfigBean configDesired = yamlProc.loadAs(new FileInputStream(
            new File("src/test/resources/basic/configDesired.yml")), ConfigBean.class);

        System.out.println(diffLogic.createDiffOutput(configLive, configDesired));
    }
}
