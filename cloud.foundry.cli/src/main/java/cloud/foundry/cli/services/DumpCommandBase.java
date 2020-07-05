package cloud.foundry.cli.services;

import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;

import java.util.concurrent.Callable;

public class DumpCommandBase<B extends Bean> implements Callable<Integer> {

    @Mixin
    private YamlCommandOptions yamlCommandOptions;

    private Class<B> beanClass;
    private Log log;

    /**
     * TODO
     * @param beanClass
     * @param commandClass
     * @param <C>
     */
    protected <C extends DumpCommandBase<B>> DumpCommandBase(Class<B> beanClass, Class<C> commandClass) {
        this.beanClass = beanClass;
        log = Log.getLog(commandClass);
    }

    @Override
    public Integer call() throws Exception {
        log.info("Reading and processing YAML file...");
        String yamlFilePath = yamlCommandOptions.getYamlFilePath();
        String resolvedConfig = YamlMapper.resolveYamlFile(yamlFilePath);
        System.out.println(resolvedConfig);
        YamlMapper.interpretBean(resolvedConfig, beanClass);
        return 0;
    }

}