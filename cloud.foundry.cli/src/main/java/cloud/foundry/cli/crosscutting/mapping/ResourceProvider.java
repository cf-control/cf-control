package cloud.foundry.cli.crosscutting.mapping;

import java.io.InputStream;

/**
 * Class used for accessing information from property files.
 */
public class ResourceProvider {

    /**
     * Get an Input stream from a resource File. Needs to be closed after usage.
     * @param filename the Filename of the resource file
     * @param forClass the class, which needs the InputStream
     * @return an InputStream to the resource file
     */
    public InputStream getInputStreamFromResourceFile(String filename, Class<?> forClass){
        ClassLoader classLoader = forClass.getClassLoader();
        return classLoader.getResourceAsStream(filename);
    }
}
