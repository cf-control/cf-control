package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;

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
