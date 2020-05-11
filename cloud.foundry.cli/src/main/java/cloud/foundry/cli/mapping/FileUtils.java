package cloud.foundry.cli.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This utility class provides a more easy to use interface for handling
 * YAML files requested from a local file source or a remote host source.
 *
 */
public class FileUtils {

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "YAML",
            "YML"
    ));

    /**
     *
     * @param filePath  a relative file path beginning from the working directory
     * @return          the content of the file as a String
     * @throws IOException
     */
    public static String readLocalFile(String filePath) throws IOException {
        checkNotNull(filePath);

        String content = doReadLocalFile(filePath);

        assert content != null;
        return content;
    }

    private static String doReadLocalFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!isYamlFile(file.getName())) {
            throw new InvalidFileTypeException("invalid or missing file extension: "
                    + FilenameUtils.getExtension(file.getName()));
        }
        return readFile(file);
    }

    /**
     *
     * @param url   url to the host e.g. https://host.com/path/to/file.yml
     * @return      the content of the file as a String
     * @throws IOException
     */
    public static String readRemoteFile(String url) throws IOException {
        checkNotNull(url);

        String content = doReadRemoteFile(url);

        assert content != null;
        return content;
    }

    private static String doReadRemoteFile(String url) throws IOException {
        return null;
    }

    public static boolean isYamlFile(String filename) {
        return ALLOWED_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(filename).toUpperCase());
    }

    private static String readFile(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

}
