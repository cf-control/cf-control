package cloud.foundry.cli.crosscutting.util;

import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ServiceBean;
import cloud.foundry.cli.crosscutting.mapping.beans.SpaceDevelopersBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ConfigBean;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class YamlCreator {
    /**
     * Factory function creating a yaml processor with common options for dumping
     * (ensures a consistent output format).
     *
     * @return yaml processor preconfigured with proper dumping options
     */
    public static Yaml createDefaultYamlProcessor() {
        DumperOptions options = new DumperOptions();
        // do not dump tags into the document
        options.setTags(new HashMap<>());
        // format all nested mappings in block style
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // indentation aids readability
        options.setIndent(2);
        // use custom representer to hide bean class names in output
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
                                                          Object propertyValue,Tag customTag) {
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
        representer.addClassTag(ConfigBean.class, Tag.MAP);
        return new Yaml(representer, options);
    }
}
