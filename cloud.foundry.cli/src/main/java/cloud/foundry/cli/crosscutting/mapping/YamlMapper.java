package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.RefResolvingException;
import cloud.foundry.cli.crosscutting.exceptions.YamlParsingException;
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
import org.yaml.snakeyaml.error.MarkedYAMLException;
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

    private static <B extends Bean> B loadBean(Object yamlTreeRoot, Class<B> beanType, String rootFilePath) {
        // resolving is not supported if no parent file path is defined
        if (rootFilePath != null) {
            yamlTreeRoot = RefResolver.resolveRefs(yamlTreeRoot, rootFilePath);
        }

        String resolvedConfig = dump(yamlTreeRoot);

        return interpretBean(resolvedConfig, beanType);
    }

    /**
     * Loads a configuration file as a bean. During this process, the ref-occurrences in the specified configuration
     * file are resolved.
     *
     * @param configFilePath the path to the config file
     * @param beanType the desired type of the bean to load
     * @return a bean holding the information of the configuration file
     * @throws IOException if the config file cannot be accessed
     * @throws RefResolvingException if an error related to the ref-resolution process occurs
     * @throws YamlParsingException if the configuration file content cannot be parsed as yaml or
     *                              if the resolved config cannot be interpreted as the given bean type
     */
    public static <B extends Bean> B loadBeanFromFile(String configFilePath, Class<B> beanType) throws IOException {
        Object yamlTreeRoot = loadYamlTreeFromFilePath(configFilePath);
        return loadBean(yamlTreeRoot, beanType, configFilePath);
    }

    /**
     * Loads a YAML string as a bean. During this process, the ref-occurrences in the specified configuration file
     * are resolved.
     *
     * @param data YAML data
     * @param beanType the desired type of the bean to load
     * @return a bean holding the information of the configuration file
     * @throws RefResolvingException if an error related to the ref-resolution process occurs
     * @throws YamlParsingException if the configuration file content cannot be parsed as yaml or
     *                              if the resolved config cannot be interpreted as the given bean type
     */
    public static <B extends Bean> B loadBeanFromString(String data, Class<B> beanType) {
        Object yamlTreeRoot = loadYamlTreeFromYamlString(data);
        return loadBean(yamlTreeRoot, beanType, null);
    }

    /**
     * Loads a yaml file as a tree and resolves all ref-occurrences. Returns the resolved content in the
     * yaml format.
     *
     * @param yamlFilePath the path a yaml file
     * @return the resolved content of the yaml file in the yaml format
     * @throws IOException if the yaml file cannot be accessed
     * @throws YamlParsingException if the configuration file content cannot be parsed as yaml
     * @throws RefResolvingException if an error related to the ref-resolution process occurs
     */
    public static String resolveYamlFile(String yamlFilePath) throws IOException {
        Object yamlTreeRoot = loadYamlTreeFromFilePath(yamlFilePath);
        yamlTreeRoot = RefResolver.resolveRefs(yamlTreeRoot, yamlFilePath);

        return dump(yamlTreeRoot);
    }

    /**
     * Interprets a string that is formatted in the yaml format as a desired bean object. Ref-occurrences in the
     * provided yaml content should have been resolved beforehand.
     *
     * @param resolvedYamlContent the yaml content to be interpreted as a bean object
     * @param beanType the desired type of the bean
     * @return the resolved content of the yaml file in the yaml format
     * @throws IllegalArgumentException if the configuration file content cannot be parsed as yaml
     * @throws YamlParsingException if the resolved config cannot be interpreted as the given bean type
     */
    public static <B extends Bean> B interpretBean(String resolvedYamlContent, Class<B> beanType) {
        Yaml yamlProcessor = new Yaml();

        try {
            return yamlProcessor.loadAs(resolvedYamlContent, beanType);
        } catch (ConstructorException constructorException) {
            // special treatment for the constructor exception because it denotes an actual interpretation failure
            throw new YamlParsingException(constructorException);
        } catch (MarkedYAMLException markedYamlException) {
            // it is expected that the provided yaml content has a yaml format, that can be parsed by the yaml library
            throw new IllegalArgumentException("Unable to parse the resolved yaml content", markedYamlException);
        }
    }

    /**
     * Reads the file (possibly on a server) and interprets its content as a yaml tree.
     *
     * @param filePath the path or url to a file
     * @return the resulting yaml tree
     * @throws IOException if the file cannot be accessed
     * @throws YamlParsingException if the configuration file content cannot be parsed as yaml
     */
    static Object loadYamlTreeFromFilePath(String filePath) throws IOException {
        try (InputStream inputStream = FileUtils.openLocalOrRemoteFile(filePath)) {
            try {
                return createTreeLoader().load(inputStream);
            } catch (MarkedYAMLException markedYamlException) {
                throw new YamlParsingException(markedYamlException, filePath);
            }
        }
    }

    /**
     * Interprets the given string as a yaml tree.
     *
     * @param data the yaml data as a string
     * @return the resulting yaml tree
     * @throws YamlParsingException if the entered string cannot be parsed as yaml
     */
    static Object loadYamlTreeFromYamlString(String data) {
        try {
            return createTreeLoader().load(data);
        } catch (MarkedYAMLException markedYamlException) {
            throw new YamlParsingException(markedYamlException, "the provided string");
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
}
