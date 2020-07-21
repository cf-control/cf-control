package cloud.foundry.cli.crosscutting.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Test for {@link VersionPropertiesFileUtils}
 */
public class VersionPropertiesFileUtilsTest {

    @Test
    public void testDetermineApiVersionSucceeds() throws IOException {
        // given
        ResourceProvider resourceProviderMock = mock(ResourceProvider.class);
        Properties propertiesMock = mock(Properties.class);
        InputStream inputStreamMock = mock(InputStream.class);
        when(resourceProviderMock.getInputStreamFromResourceFile(any(), any()))
                .thenReturn(inputStreamMock);
        when(propertiesMock.getProperty("version")).thenReturn("1.0.0");

        // when
        String version = VersionPropertiesFileUtils.determineApiVersion(resourceProviderMock, propertiesMock);

        // then
        verify(propertiesMock).load(inputStreamMock);
        assertThat(version, is("1.0.0"));
    }

    @Test
    public void testDetermineApiVersionWithPropertiesLoadThrowingIoExceptionReturnsNotFound() throws IOException {
        // given
        ResourceProvider resourceProviderMock = mock(ResourceProvider.class);
        Properties propertiesMock = mock(Properties.class);
        InputStream stubInputStream =
                IOUtils.toInputStream("some test data for my input stream", "UTF-8");
        when(resourceProviderMock.getInputStreamFromResourceFile(any(), any()))
                .thenReturn(stubInputStream);
        doThrow(new IOException()).when(propertiesMock).load(stubInputStream);
        // when
        String version = VersionPropertiesFileUtils.determineApiVersion(resourceProviderMock, propertiesMock);

        // then
        verify(propertiesMock).load(stubInputStream);
        assertThat(version, is("NOT_FOUND"));
    }

}
