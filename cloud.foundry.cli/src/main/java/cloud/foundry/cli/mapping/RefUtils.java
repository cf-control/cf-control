package cloud.foundry.cli.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class RefUtils {

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "YAML",
            "YML"
    ));

    public static String readLocalFile(String filePath) throws IOException {
        checkNotNull(filePath);

        String content = doReadLocalFile(filePath);

        assert content != null;
        return content;
    }

    private static String doReadLocalFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!isYamlFile(file.getName())) {
            throw new InvalidFileTypeException();
        }
        return readFile(file);
    }

    private static boolean isYamlFile(String filename) {
        return ALLOWED_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(filename).toUpperCase());
    }

    private static String readFile(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return IOUtils.toString(in, Charset.defaultCharset());
        }
    }

}
