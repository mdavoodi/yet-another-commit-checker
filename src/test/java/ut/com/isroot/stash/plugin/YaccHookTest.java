package ut.com.isroot.stash.plugin;

import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.YaccHook;
import com.isroot.stash.plugin.YaccService;
import com.isroot.stash.plugin.errors.YaccError;
import com.isroot.stash.plugin.errors.YaccErrorBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2014-01-15
 */
public class YaccHookTest {
    @Mock private YaccService yaccService;
    @Mock private HookResponse hookResponse;
    @Mock private RepositoryHookContext repositoryHookContext;
    @Mock private Settings settings;

    private StringWriter errorMessage;

    private YaccHook yaccHook;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        yaccHook = new YaccHook(yaccService);

        errorMessage = new StringWriter();

        when(hookResponse.err()).thenReturn(new PrintWriter(errorMessage));
        when(repositoryHookContext.getSettings()).thenReturn(settings);
    }

    @Test
    public void testOnReceive_deleteRefChangesIgnored() {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getType()).thenReturn(RefChangeType.DELETE);

        boolean allowed = yaccHook.onReceive(repositoryHookContext, Lists.newArrayList(refChange), null);
        assertThat(allowed).isTrue();
        verifyZeroInteractions(yaccService);
    }

    @Test
    public void testOnReceive_NullRefChangesIgnored() {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        when(refChange.getToHash()).thenReturn("0000000000000000000000000000000000000000");

        boolean allowed = yaccHook.onReceive(repositoryHookContext, Lists.newArrayList(refChange), null);
        assertThat(allowed).isTrue();
        verifyZeroInteractions(yaccService);
    }

    @Test
    public void testOnReceive_pushRejectedIfThereAreErrors() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error with commit")));

        boolean allowed = yaccHook.onReceive(repositoryHookContext, Lists.newArrayList(mock(RefChange.class)), hookResponse);
        assertThat(allowed).isFalse();
    }

    @Test
    public void testOnReceive_errorsArePrintedToHookStdErr() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1"), new YaccError("error2")));

        yaccHook.onReceive(repositoryHookContext, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString())
                .isEqualTo(YaccErrorBuilder.ERROR_BEARS + "\n" +
                        "\n" +
                        "refs/heads/master: error1\n" +
                        "\n" +
                        "refs/heads/master: error2\n" +
                        "\n");
    }

    @Test
    public void testOnReceive_defaultHeaderDisplayedIfErrorMessageHeaderIsEmpty() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1")));

        when(settings.getString("errorMessageHeader")).thenReturn("");

        yaccHook.onReceive(repositoryHookContext, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString()).startsWith(YaccErrorBuilder.ERROR_BEARS);
    }

    @Test
    public void testOnReceive_nonEmptyErrorMessageHeaderReplacesDefaultHeader() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1")));

        when(settings.getString("errorMessageHeader")).thenReturn("Custom Header");

        yaccHook.onReceive(repositoryHookContext, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString()).isEqualTo("Custom Header\n" +
                "\n" +
                "refs/heads/master: error1\n\n");
    }

    @Test
    public void testOnReceive_errorMessageFooterAddedToEndOfOutput() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1")));

        when(settings.getString("errorMessageFooter")).thenReturn("Custom Footer");

        yaccHook.onReceive(repositoryHookContext, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString()).endsWith("\nCustom Footer\n\n");
    }

    private List<RefChange> getMockRefChanges() {
        List<RefChange> refChanges = new ArrayList<>();
        RefChange refChange = mock(RefChange.class);

        when(refChange.getRefId()).thenReturn("refs/heads/master");

        refChanges.add(refChange);
        return refChanges;
    }
}
