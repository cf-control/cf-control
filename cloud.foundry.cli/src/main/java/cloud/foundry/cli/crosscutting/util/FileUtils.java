package cloud.foundry.cli.crosscutting.util;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.InvalidFileTypeException;
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
     * @throws IOException if the file cannot be accessed
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
            throw new InvalidFileTypeException("invalid file extension: "
                    + FilenameUtils.getExtension(file.getName()));
        } else if (emptyFileExtension(file.getName())) {
            throw new InvalidFileTypeException("missing file extension");
        }
        return readFile(file);
    }

    private static boolean isYamlFile(String filename) {
        return ALLOWED_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(filename).toUpperCase());
    }

    public static boolean emptyFileExtension(String filename) {
        return FilenameUtils.getExtension(filename).isEmpty();
    }

    private static String readFile(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            //TODO: return InputStream object instead of a string
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

}
