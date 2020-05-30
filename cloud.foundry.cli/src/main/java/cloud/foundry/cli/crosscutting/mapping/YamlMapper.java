package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;
import org.yaml.snakeyaml.Yaml;

public class YamlMapper {

    public static <B extends Bean> B loadBean(String configFilePath, Class<B> beanType) {
        //TODO
        return null;
    }

    public static <B extends Bean> String dumpBean(B bean) {
        //TODO
        return null;
    }

    public static Object resolve(Object yamlTreeRoot, Yaml yamlParser) {
        // TODO custom exceptions in case of error:
        // e.g. unknown node type
        return null;
    }
}
