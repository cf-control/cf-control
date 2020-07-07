package cloud.foundry.cli.services;

import static picocli.CommandLine.Mixin;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.Bean;

import java.util.concurrent.Callable;

/**
 * The base class for concrete dump commands. It implements the process of resolving and interpreting the provided
 * configuration file via a {@link Callable<Integer>} interface (as any other command class).
 * @param <B> the bean type in which the resolved config will be interpreted
 */
public abstract class DumpCommandBase<B extends Bean> implements Callable<Integer> {

    @Mixin
    private YamlCommandOptions yamlCommandOptions;

    private Class<B> beanClass;
    private Log log;

    /**
     * Constructs the command instance by the class object of the bean type, that is used in this class, and the class
     * object of the concrete derived class. This is needed for retrieving the according {@link Log log} instance.
     * @param beanClass the class object of the bean type
     * @param commandClass the class object of the dump command class that derived from this class
     * @param <C> the type of the dump command class that derived from this class
     */
    protected <C extends DumpCommandBase<B>> DumpCommandBase(Class<B> beanClass, Class<C> commandClass) {
        this.beanClass = beanClass;
        log = Log.getLog(commandClass);
    }

    /**
     * Implementation of the dump command functionality. It reads the provided configuration file, resolves
     * ref-occurrences, prints the resolved configuration and tries to interpret it as the bean type, that is specified
     * in the generic type of this class.
     */
    @Override
    public Integer call() throws Exception {
        String yamlFilePath = yamlCommandOptions.getYamlFilePath();

        log.info("Reading and processing YAML file...");
        String resolvedConfig = YamlMapper.resolveYamlFile(yamlFilePath);

        // print the config before interpreting it, so that in cases of interpretation errors, the user still gets his
        // resolved config printed out on the console
        System.out.println(resolvedConfig);

        YamlMapper.interpretBean(resolvedConfig, beanClass);
        return 0;
    }

}
