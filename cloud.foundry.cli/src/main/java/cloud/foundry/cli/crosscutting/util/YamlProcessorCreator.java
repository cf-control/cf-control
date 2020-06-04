package cloud.foundry.cli.crosscutting.util;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.HashMap;

public class YamlProcessorCreator {

    /**
     * Factory function creating a yaml processor with common options for dumping
     * (ensures a consistent output format).
     *
     * @return yaml processor preconfigured with proper dumping options
     */
    public static Yaml createDefault() {
        DumperOptions options = createDefaultDumperOptions();

        // use custom representer to hide bean class names in output
        // we explicitly have to add _all_ custom bean types
        Representer representer = new Representer();
        addBeanClassTags(representer);

        return new Yaml(representer, options);
    }


    /**
     * Factory function creating a yaml processor with that ignores null values when parsing
     * and with common options for dumping
     * (ensures a consistent output format).
     *
     * @return yaml processor preconfigured with proper dumping options
     */
    public static Yaml createNullValueIgnoring() {
        DumperOptions options = createDefaultDumperOptions();

        // use custom representer to hide bean class names in output
        // we explicitly have to add _all_ custom bean types
        Representer representer = createIgnoreNullValueRepresenter();
        addBeanClassTags(representer);

        return new Yaml(representer, options);
    }

    private static DumperOptions createDefaultDumperOptions() {
        DumperOptions options = new DumperOptions();
        // do not dump tags into the document
        options.setTags(new HashMap<>());
        // format all nested mappings in block style
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // indentation aids readability
        options.setIndent(2);
        return options;
    }

    private static Representer createIgnoreNullValueRepresenter() {
        return new Representer() {

            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean,
                                                          Property property,
                                                          Object propertyValue,
                                                          Tag customTag) {
                if (propertyValue == null) {
                    return null;
                }
                else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }

        };
    }

    private static void addBeanClassTags(Representer representer) {
        representer.addClassTag(ApplicationBean.class, Tag.MAP);
        representer.addClassTag(ApplicationManifestBean.class, Tag.MAP);
        representer.addClassTag(ServiceBean.class, Tag.MAP);
        representer.addClassTag(SpaceDevelopersBean.class, Tag.MAP);
        representer.addClassTag(ConfigBean.class, Tag.MAP);
        representer.addClassTag(SpecBean.class, Tag.MAP);
        representer.addClassTag(TargetBean.class, Tag.MAP);
    }
}
