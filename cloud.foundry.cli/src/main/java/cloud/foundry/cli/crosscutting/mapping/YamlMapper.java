package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.beans.Bean;

public class YamlMapper {

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

    public static <B extends Bean> String dumpBean(B bean) {
        //TODO
        return null;
    }
}
