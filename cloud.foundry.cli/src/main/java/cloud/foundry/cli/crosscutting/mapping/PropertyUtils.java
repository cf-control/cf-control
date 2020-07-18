package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utils class for reading information from property files.
 */
public class PropertyUtils {

    private static final Log log = Log.getLog(PropertyUtils.class);

    /**
     * Name of the property file, which contains the version of our application
     */
    private static final String VERSION_PROPERTIES = "version.properties";

    /**
     * The key of the version in the properties file
     */
    private static final String VERSION = "version";


    /**
     * Determines the API-Version from our application
     * If it fails, the API Version will be marked as NOT_FOUND and the error will be logged.
     *
     * @return API-Version
     */
    public String determineApiVersion(ResourceProvider resourceProvider, Properties properties) {
        InputStream inputStream = resourceProvider.getInputStreamFromResourceFile(VERSION_PROPERTIES, this.getClass());
        try {
            properties.load(inputStream);
            String value = properties.getProperty(VERSION);
            return value;
        } catch (IOException e) {
            log.error("Could not read the api version", e.getMessage());
            return "NOT_FOUND";
        }
    }

}
