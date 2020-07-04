package cloud.foundry.cli.services.dump;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.RefResolver;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;
import cloud.foundry.cli.services.YamlCommandOptions;
import picocli.CommandLine;

import java.util.concurrent.Callable;

public class AbstractDumpCommand<B extends Bean> implements Callable<Integer> {

    @CommandLine.Mixin
    YamlCommandOptions yamlCommandOptions;

    private Log log;
    private Class<B> beanClass;

    /**
     * TODO
     * @param beanClass
     * @param commandClass
     * @param <C>
     */
    protected <C extends AbstractDumpCommand> AbstractDumpCommand(Class<B> beanClass, Class<C> commandClass) {
        this.beanClass = beanClass;
        log = Log.getLog(commandClass);
    }

    @Override
    public Integer call() throws Exception {
        log.info("Reading YAML file and resolving,", RefResolver.REF_KEY + "-occurrences...");
        String yamlFilePath = yamlCommandOptions.getYamlFilePath();
        String resolvedConfig = YamlMapper.resolveYamlFile(yamlFilePath);
        System.out.println(resolvedConfig);
        YamlMapper.interpretBean(resolvedConfig, beanClass);
        return 0;
    }

}