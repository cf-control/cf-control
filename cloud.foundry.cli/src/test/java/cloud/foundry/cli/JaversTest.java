package cloud.foundry.cli;

import cloud.foundry.cli.crosscutting.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.util.YamlProcessorCreator;
import cloud.foundry.cli.logic.DiffNode;
import cloud.foundry.cli.logic.Differ;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class JaversTest {

    @Test
    public void testJaversSimple() throws FileNotFoundException {
        Yaml yamlProc = YamlProcessorCreator.createNullValueIgnoring();

        ConfigBean configLive = yamlProc.loadAs(new FileInputStream(
            new File("src/test/resources/basic/configLive.yml")), ConfigBean.class);
        ConfigBean configDesired = yamlProc.loadAs(new FileInputStream(
            new File("src/test/resources/basic/configDesired.yml")), ConfigBean.class);

        DiffNode diffTree = Differ.createDiffTree(configLive, configDesired);

        System.out.println(diffTree.toDiffString());
    }
}
