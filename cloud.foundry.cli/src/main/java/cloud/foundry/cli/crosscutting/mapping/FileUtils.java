package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.exceptions.InvalidFileTypeException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class provides a more easy to use interface for handling
 * YAML files requested from a local file source or a remote host source.
 */
public class FileUtils {

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "YAML",
            "YML"
    ));

    /**
     * Opens a file with the given path on the local file system or on the remote host.
     * The user must make sure to close the InputStream after usage.
     * @param filepath  a relative file path beginning from the working directory or a url
     * @return          the content of the file as a InputStream
     * @throws IOException if the file cannot be accessed
     * <br>or the connection was aborted
     * <br>or the response code was not valid
     * <br>or when there was an error retrieving the input stream of the http response content
     */
    public static InputStream openLocalOrRemoteFile(String filepath) throws IOException {
        checkNotNull(filepath);

        if (isRemoteFile(filepath)) {
            return doOpenRemoteFile(filepath);
        } else {
            return doOpenLocalFile(filepath);
        }
    }

    private static boolean isRemoteFile(String filepath) {
        Pattern p = Pattern.compile("\\w+?://");
        Matcher m = p.matcher(filepath);
        return m.find();
    }


    /**
     * Opens a file with the given path on the local file system.
     * The user must make sure to close the InputStream after usage.
     * @param filepath  a relative file path beginning from the working directory
     * @return          the content of the file as a InputStream
     * @throws IOException if the file cannot be accessed
     */
    public static InputStream openLocalFile(String filepath) throws IOException {
        checkNotNull(filepath);

        return doOpenLocalFile(filepath);
    }

    private static InputStream doOpenLocalFile(String filepath) throws IOException {
        File file = new File(filepath);
        checkFileExtensionNotEmpty(file.getName());
        checkHasAllowedFileExtension(file.getName());

        return new FileInputStream(file);
    }


    /**
     * Opens a file with the given path on a remote host. The user must make sure to close the InputStream after usage.
     * @param url   url to the host e.g.
     *             <br> https://host.com/path/to/file.yml
     * @return      the content of the file as a InputStream
     * @throws IOException  in case of a problem or the connection was aborted
     * <br>or the response code was not valid
     * <br>or when there was an error retrieving the input stream of the http response content
     *
     */
    public static InputStream openRemoteFile(String url) throws IOException {
        checkNotNull(url);

        return doOpenRemoteFile(url);
    }

    private static InputStream doOpenRemoteFile(String url) throws IOException {
        URI uri = URI.create(url);

        checkHasAllowedFileExtension(uri.getPath());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(uri))) {

                if (response.getCode() != HttpStatus.SC_SUCCESS) {
                    throw new HttpResponseException(response.getCode(), response.getReasonPhrase());
                }

                if (response.getEntity() == null || response.getEntity().getContent() == null) {
                    throw new IOException("No response content input stream available.");
                }

                // cloning the input stream, since leaving the ClosableHttpResponse block
                // will automatically close the the underlying content input stream
                return cloneInputStream(response.getEntity().getContent());
            }
        }
    }

    private static InputStream cloneInputStream(InputStream inputStream) throws IOException {
        byte[] data = IOUtils.toByteArray(inputStream);
        return new ByteArrayInputStream(data);
    }

    /**
     * Calculate absolute path for file, relative to a given directory. If the path is absolute already, it is
     * returned as-is. URLs are ignored, too.
     * @param potentiallyRelativePath a path that might be absolute
     * @param parentDirectoryPath directory from which to resolve the file path
     * @return absolute path
     */
    public static String calculateAbsolutePath(
            @Nonnull final String potentiallyRelativePath,
            @Nonnull final String parentDirectoryPath)
    {
        checkNotNull(potentiallyRelativePath);
        checkNotNull(parentDirectoryPath);

        // ignore URIs
        if (potentiallyRelativePath.contains("://")) {
            return potentiallyRelativePath;
        }

        // ignore absolute paths
        if (Paths.get(potentiallyRelativePath).isAbsolute()) {
            return potentiallyRelativePath;
        }

        return Paths.get(parentDirectoryPath, potentiallyRelativePath).toAbsolutePath().toString();
    }

    private static void checkFileExtensionNotEmpty(String name) throws InvalidFileTypeException {
        if (FilenameUtils.getExtension(name).isEmpty()) {
            throw new InvalidFileTypeException("Invalid file extension: no file extension.");
        }
    }

    private static void checkHasAllowedFileExtension(String name) throws InvalidFileTypeException {
        if (!FilenameUtils.getExtension(name).isEmpty()
                && !ALLOWED_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(name).toUpperCase())) {
            throw new InvalidFileTypeException("Invalid file extension. Was "
                    + FilenameUtils.getExtension(name) + ", allowed are " +
                    ALLOWED_FILE_EXTENSIONS + ".");
        }
    }
}
