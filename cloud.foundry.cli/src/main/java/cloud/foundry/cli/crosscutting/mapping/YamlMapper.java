package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;

public class YamlMapper {

    public static <B extends Bean> B loadBean(String configFilePath, Class<B> beanType) {
        //TODO
        /*
        Open Config file.
        Parse file content as YML Tree.
        Resolve Ref occurences in YML Tree.
        Dump YML Tree as String.
        Load String as bean.
         */
        try {
            String configFile = FileUtils.readLocalFile(configFilePath);
            Yaml yaml = new Yaml();
            Object tree = yaml.load(configFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <B extends Bean> String dumpBean(B bean) {
        //TODO
        return null;
    }

    public static Object resolve(Object yamlTreeRoot, Yaml yamlParser) {
        // TODO custom exceptions in case of error:
        // e.g. unknown node type
        Log.debug("Resolve", RefResolvingYamlTreeVisitor.REF_INDICATOR + "-occurrences");
        RefResolvingYamlTreeVisitor refResolvingYamlTreeVisitor =
                new RefResolvingYamlTreeVisitor(yamlTreeRoot, yamlParser);
        Object resolvedYamlTreeRoot = refResolvingYamlTreeVisitor.resolveRefs();
        Log.debug("Resolving completed");
        return resolvedYamlTreeRoot;

    }
}
