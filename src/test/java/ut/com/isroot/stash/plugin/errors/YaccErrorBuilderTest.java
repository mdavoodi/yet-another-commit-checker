package ut.com.isroot.stash.plugin.errors;

import com.atlassian.stash.setting.Settings;
import com.isroot.stash.plugin.errors.YaccError;
import com.isroot.stash.plugin.errors.YaccErrorBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2015-04-26
 */
public class YaccErrorBuilderTest {

    private Settings settings;
    private YaccErrorBuilder yaccErrorBuilder;

    @Before
    public void setup() {
        settings = mock(Settings.class);

        yaccErrorBuilder = new YaccErrorBuilder(settings);
    }

    @Test
    public void testGetErrorMessage_defaultHeader() {
        String message = yaccErrorBuilder.getErrorMessage(new ArrayList<YaccError>());

        assertThat(message).isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n\n");
    }

    @Test
    public void testGetErrorMessage_defaultHeaderShownIfCustomizedHeaderIsEmpty() {
        when(settings.getString("errorMessageHeader")).thenReturn("");

        String message = yaccErrorBuilder.getErrorMessage(new ArrayList<YaccError>());

        assertThat(message).isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n\n");

        verify(settings).getString("errorMessageHeader");
    }

    @Test
    public void testGetErrorMessage_headerIsOverriddenIfCustomizedHeaderIsPresent() {
        when(settings.getString("errorMessageHeader")).thenReturn("custom header");

        String message = yaccErrorBuilder.getErrorMessage(new ArrayList<YaccError>());

        assertThat(message).isEqualTo("custom header\n\n");
    }

    @Test
    public void testGetErrorMessage_commitErrorsAreShown() {
        List<YaccError> errors = new ArrayList<>();
        errors.add(new YaccError("commit error"));

        String message = yaccErrorBuilder.getErrorMessage(errors);

        assertThat(message).isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n\n"
                + "commit error\n"
                + "\n");
    }

    @Test
    public void testGetErrorMessage_multipleErrors() {
        List<YaccError> errors = new ArrayList<>();
        errors.add(new YaccError("commit error"));
        errors.add(new YaccError("another error"));

        String message = yaccErrorBuilder.getErrorMessage(errors);

        assertThat(message).isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n\n"
                + "commit error\n"
                + "\n"
                + "another error\n"
                + "\n");
    }

    @Test
    public void testGetErrorMessage_customFooterIsShownIfPresent() {
        when(settings.getString("errorMessageFooter")).thenReturn("custom footer");

        List<YaccError> errors = new ArrayList<>();
        errors.add(new YaccError("commit error"));

        String message = yaccErrorBuilder.getErrorMessage(errors);

        assertThat(message).isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n\n"
                + "commit error\n"
                + "\n"
                + "custom footer\n"
                + "\n");
    }

    @Test
    public void testGetErrorMessage_customErrorsShownIfPresent() {
        when(settings.getString("errorMessage.COMMIT_REGEX")).thenReturn("more info");

        List<YaccError> errors = new ArrayList<>();
        errors.add(new YaccError(YaccError.Type.COMMIT_REGEX, "commit error"));

        String message = yaccErrorBuilder.getErrorMessage(errors);

        assertThat(message).isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n\n"
                + "commit error\n"
                + "\n"
                + "    more info\n"
                + "\n");
    }
}