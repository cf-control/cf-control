package cloud.foundry.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import java.util.List;

public class GreetTest {

    @Test
    public void testEquals() {
        String theBiscuit = "Ginger";
        String myBiscuit = "Ginger";
        assertThat(theBiscuit, is(myBiscuit));
    }

    @Test
    public void testMock() {
        // mock creation
        List mockedList = mock(List.class);

        // using mock object - it does not throw any "unexpected interaction" exception
        mockedList.add("one");
        mockedList.clear();

        // selective, explicit, highly readable verification
        verify(mockedList).add("one");
        verify(mockedList).clear();
    }
}