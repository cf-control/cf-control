package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class takes care about loading {@link Bean beans} from configuration files and dumping {@link Bean beans} as
 * {@link String strings}. It guarantees consistent loading and dumping.
 */
public class YamlMapper {

    /**
     * The indentation (number of white spaces) used for yaml dumping.
     */
    public static final int INDENTATION = 2;

    /**
     * Loads a configuration file as a bean. During this process, the ref-occurrences in the specified configuration
     * file are resolved.
     *
     * @param configFilePath the path to the config file
     * @param beanType the desired type of the bean to load
     * @throws IOException if the config file cannot be accessed
     * @throws RefResolvingException if an error during the ref-resolution process occurs
     * @throws ConstructorException if the resolved Object can not be dumped as the given Bean type
     */
    public static <B extends Bean> B loadBean(String configFilePath, Class<B> beanType) throws IOException {
        Object yamlTreeRoot = loadYamlTree(configFilePath);
        yamlTreeRoot = RefResolver.resolveRefs(yamlTreeRoot);

        Yaml treeDumper = createMinimalDumper();
        String resolvedConfig = treeDumper.dump(yamlTreeRoot);

        Yaml yamlProcessor = new Yaml();
        return yamlProcessor.loadAs(resolvedConfig, beanType);
    }

    /**
     * Reads the file (possibly on a server) and interprets its content as a yaml tree.
     *
     * @param filePath the path or url to a file
     * @return the resulting yaml tree
     * @throws IOException if the file cannot be accessed
     */
    static Object loadYamlTree(String filePath) throws IOException {
        try (InputStream inputStream = FileUtils.openLocalOrRemoteFile(filePath)) {
            return createTreeLoader().load(inputStream);
        }
    }

    private static Yaml createTreeLoader() {
        return new Yaml(new SafeConstructor());
    }

    /**
     * Dumps the contents of an arbitrary object as a string in the yaml format.
     *
     * @param object the object to dump
     * @return the contents of the parameter as a yaml string
     */
    public static String dump(Object object) {
        return dump(object, createDefaultDumper());
    }

    private static String dump(Object object, Yaml yamlProcessor) {
        return yamlProcessor.dump(object);
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
        options.setIndent(INDENTATION);
        // use custom representer to hide bean class names in output
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                                                          Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    // if value of property is null, ignore it.
                    return null;
                } else if (propertyValue instanceof Collection && ((Collection) propertyValue).isEmpty()) {
                    // if the content of the property is empty, ignore it.
                    return null;
                } else if (propertyValue instanceof Map && ((Map) propertyValue).isEmpty()) {
                    // if the content of the property is empty, ignore it.
                    return null;
                }
                else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };
        // we explicitly have to add _all_ custom bean types
        representer.addClassTag(ApplicationBean.class, Tag.MAP);
        representer.addClassTag(ApplicationManifestBean.class, Tag.MAP);
        representer.addClassTag(ServiceBean.class, Tag.MAP);
        representer.addClassTag(SpaceDevelopersBean.class, Tag.MAP);
        representer.addClassTag(SpecBean.class, Tag.MAP);
        representer.addClassTag(TargetBean.class, Tag.MAP);
        representer.addClassTag(ConfigBean.class, Tag.MAP);
        return new Yaml(representer, options);
    }

    private static Yaml createMinimalDumper() {
        DumperOptions options = new DumperOptions();
        // do not dump tags into the document
        options.setTags(new HashMap<>());
        // minimal indentation needed as this does not serve as output to the user
        options.setIndent(1);
        return new Yaml(options);
    }
}
