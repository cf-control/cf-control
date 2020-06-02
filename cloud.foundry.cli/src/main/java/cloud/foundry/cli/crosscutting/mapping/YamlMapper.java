package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;
import cloud.foundry.cli.crosscutting.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.GetAllBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * This class takes care about loading {@link Bean beans} from configuration files and dumping {@link Bean beans} as
 * {@link String strings}. It guarantees consistent loading and dumping.
 */
public class YamlMapper {

    /**
     * Loads a configuration file as a bean. During this process, the ref-occurrences in the specified configuration
     * file are resolved.
     *
     * @param configFilePath the path to the config file
     * @param beanType the desired type of the bean to load
     * @throws IOException if the config file cannot be accessed
     * @throws RefResolvingException if an error during the ref-resolution process occurs
     * @throws ConstructorException if the resolved Object can not be dumped as the given Bean type
     * TODO More Exceptions?
     */
    public static <B extends Bean> B loadBean(String configFilePath, Class<B> beanType) throws IOException {
        Object yamlTreeRoot = loadYamlTree(configFilePath);
        yamlTreeRoot = RefResolver.resolveRefs(yamlTreeRoot);
        Yaml treeDumper = createTreeDumper();
        String resolvedConfig = treeDumper.dump(yamlTreeRoot);
        Yaml beanLoader = createBeanLoader();
        return beanLoader.loadAs(resolvedConfig, beanType);
    }

    /**
     * Reads the file (possibly on a server) and interprets its content as a yaml tree.
     *
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
     * Dumps the contents of an object as a string in the yaml format.
     *
     * @param object the instance to dump
     * @return the contents of the parameter as a string
     * @throws Exception TODO
     */
    public static String dump(Object object) {
        Yaml defaultYamlDumper = createDefaultDumper();
        return defaultYamlDumper.dump(object);
    }

    /**
     * Factory function creating a yaml dumper with common options
     * (ensures a consistent output format).
     *
     * @return yaml processor preconfigured with proper dumping options
     */
    private static Yaml createDefaultDumper() {
        DumperOptions options = new DumperOptions();
        // do not dump tags into the document
        options.setTags(new HashMap<>());
        // format all nested mappings in block style
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // indentation aids readability
        options.setIndent(2);
        // use custom representer to hide bean class names in output
        // we explicitly have to add _all_ custom bean types
        Representer representer = new Representer();
        representer.addClassTag(ApplicationBean.class, Tag.MAP);
        representer.addClassTag(ApplicationManifestBean.class, Tag.MAP);
        representer.addClassTag(ServiceBean.class, Tag.MAP);
        representer.addClassTag(SpaceDevelopersBean.class, Tag.MAP);
        representer.addClassTag(GetAllBean.class, Tag.MAP);
        return new Yaml(representer, options);
    }

    private static Yaml createTreeDumper() {
        DumperOptions options = new DumperOptions();
        // do not dump tags into the document
        options.setTags(new HashMap<>());
        // indentation aids readability
        options.setIndent(1);
        return new Yaml(options);
    }

    private static Yaml createBeanLoader() {
        return new Yaml();
    }
}
