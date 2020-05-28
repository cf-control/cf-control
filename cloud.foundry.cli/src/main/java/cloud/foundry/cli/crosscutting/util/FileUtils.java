package cloud.foundry.cli.crosscutting.util;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.InvalidFileTypeException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
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
        if (hasUnallowedFileExtension(file.getName())) {
            throw new InvalidFileTypeException("invalid file extension: "
                    + FilenameUtils.getExtension(file.getName()));
        } else if (emptyFileExtension(file.getName())) {
            throw new InvalidFileTypeException("missing file extension");
        }
        return readFile(file);
    }

    /**
     *
     * @param url   url to the host e.g. https://host.com/path/to/file.yml
     * @return      the content of the file as a String
     * @throws IOException
     */
    public static String readRemoteFile(String url) throws IOException, ProtocolException {
        checkNotNull(url);

        String content = doReadRemoteFile(url);

        assert content != null;
        return content;
    }

    private static String doReadRemoteFile(String url) throws IOException, ProtocolException {
        URI uri = URI.create(url);

        if (hasUnallowedFileExtension(uri.getPath()) && !emptyFileExtension(uri.getPath())) {
            throw new cloud.foundry.cli.exceptions.InvalidFileTypeException("invalid file extension: "
                    + FilenameUtils.getExtension(uri.getPath()));
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(uri))) {

                //TODO: more precise exception handling
                if (response.getCode() != 200) {
                    throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
                }

                if (response.getHeader("Content-Type") != null
                        && !response.getHeader("Content-Type").equals("text/plain")) {

                    throw new HttpResponseException(response.getCode(),
                            "invalid content type, was: " + response.getHeader("Content-Type"));
                }

                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private static boolean hasUnallowedFileExtension(String filename) {
        return !ALLOWED_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(filename).toUpperCase());
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