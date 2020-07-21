package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utils class for reading information from properties file.
 */
public class VersionPropertiesFileUtils {

    private static final Log log = Log.getLog(VersionPropertiesFileUtils.class);

    /**
     * Name of the properties file, which contains the version of our application
     */
    private static final String VERSION_PROPERTIES = "version.properties";

    /**
     * The key of the version in the properties file
     */
    private static final String VERSION = "version";


    /**
     * Determines the API-Version from our application
     * If it fails, the API Version will be marked as NOT_FOUND and the error will be logged.
     * @param resourceProvider the resource provider, used to get an input stream from an resource file
     * @param properties the properties object
     * @return API-Version
     */
    public static String determineApiVersion(ResourceProvider resourceProvider, Properties properties) {
        InputStream inputStream = resourceProvider.getInputStreamFromResourceFile(VERSION_PROPERTIES,
                VersionPropertiesFileUtils.class);
        try {
            properties.load(inputStream);
            return  properties.getProperty(VERSION);
        } catch (IOException e) {
            log.error("Could not read the api version", e.getMessage());
            return "NOT_FOUND";
        }
    }

}
