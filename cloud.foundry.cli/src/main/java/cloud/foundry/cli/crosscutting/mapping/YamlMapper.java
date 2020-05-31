package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;

/**
 * This class takes care about loading {@link Bean beans} from configuration files and dumping {@link Bean beans} as
 * {@link String strings}. It can be used to perform consistent loading and dumping.
 */
public class YamlMapper {

    /**
     * TODO documentation
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
     * TODO documentation
     */
    public static <B extends Bean> String dumpBean(B bean) {
        //TODO
        return null;
    }
}
