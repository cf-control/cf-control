package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class takes care about loading {@link Bean beans} from configuration files and dumping {@link Bean beans} as
 * {@link String strings}. It guarantees consistent loading and dumping.
 */
public class YamlMapper {

    /**
     * Loads a configuration file as a bean. During this process, the ref-occurrences in the specified configuration
     * file are resolved.
     * @param configFilePath the path to the config file
     * @param beanType the desired type of the bean to load
     * @throws Exception TODO
     */
    public static <B extends Bean> B loadBean(String configFilePath, Class<B> beanType) {
        //TODO
        /*
        open config file
        parse file content as yaml tree
        resolve ref occurrences in yaml tree
        dump yaml tree as String
        load the String as corresponding bean
         */
        return null;
    }

    /**
     * Reads the file (possibly on a server) and interprets its content as a yaml tree.
     * @param filePath the path or url to a file
     * @return the resulting yaml tree
     * @throws IOException if the file cannot be accessed
     */
    static Object loadYamlTree(String filePath) throws IOException {
        InputStream inputStream = FileUtils.openLocalOrRemoteFile(filePath);
        return createTreeLoader().load(inputStream);
    }

    private static Yaml createTreeLoader() {
        return new Yaml(new SafeConstructor());
    }

    /**
     * Dumps the contents of a bean as a string in the yaml format.
     * @param bean the bean instance to dump
     * @return the contents of the bean parameter as a string
     * @throws Exception TODO
     */
    public static <B extends Bean> String dumpBean(B bean) {
        //TODO
        return null;
    }
}
