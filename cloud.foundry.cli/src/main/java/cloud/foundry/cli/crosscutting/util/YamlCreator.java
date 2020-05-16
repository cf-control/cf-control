package cloud.foundry.cli.crosscutting.util;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.beans.ServiceInstanceSummaryBean;
import cloud.foundry.cli.crosscutting.beans.SpaceDevelopersBean;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.HashMap;

public class YamlCreator {

    /**
     * Factory function creating a yaml processor with common options for dumping (ensures a consistent output format).
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
        // we explicitly have to add _all_ custom bean types
        Representer representer = new Representer();
        representer.addClassTag(ApplicationBean.class, Tag.MAP);
        representer.addClassTag(ApplicationManifestBean.class, Tag.MAP);
        representer.addClassTag(ServiceInstanceSummaryBean.class, Tag.MAP);
        representer.addClassTag(SpaceDevelopersBean.class, Tag.MAP);


        return new Yaml(representer, options);

    }
}
