package ut.com.isroot.stash.plugin;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookService;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsBuilder;
import com.atlassian.stash.user.EscalatedSecurityContext;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.SecurityService;
import com.atlassian.stash.util.UncheckedOperation;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.YaccPreReceiveHook;
import com.isroot.stash.plugin.YaccService;
import com.isroot.stash.plugin.errors.YaccError;
import com.isroot.stash.plugin.errors.YaccErrorBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Jim Bethancourt
 *
 * Cloned from YaccHookTest.java and modified to test against the PreReceiveHook interface.
 */
public class YaccPreReceiveHookTest {
    @Mock private YaccService yaccService;
    @Mock private HookResponse hookResponse;
    @Mock private RepositoryHookService repositoryHookService;
    @Mock private RepositoryHook repositoryHook;
    @Mock private Repository repository;
    @Mock private SecurityService securityService;
    @Mock private EscalatedSecurityContext escalatedSecurityContext;
    @Mock private PluginSettingsFactory pluginSettingsFactory;
    @Mock private PluginSettings pluginSettings;
    @Mock private SettingsBuilder settingsBuilder;
    @Mock private Settings settings;

    private StringWriter errorMessage;
    private YaccPreReceiveHook yaccPreReceiveHook;


    @Before
    public void setup() throws Throwable {
        MockitoAnnotations.initMocks(this);

        yaccPreReceiveHook = new YaccPreReceiveHook(yaccService,
                pluginSettingsFactory, securityService, repositoryHookService);

        //mock hook retrieval
        when(securityService.withPermission(Permission.REPO_ADMIN, "Get plugin configuration"))
                .thenReturn(escalatedSecurityContext);
        when(escalatedSecurityContext.call(any(UncheckedOperation.class))).thenReturn(repositoryHook);

        when(repositoryHookService.createSettingsBuilder()).thenReturn(settingsBuilder);
        when(settingsBuilder.build()).thenReturn(settings);

        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(anyString())).thenReturn(settings);

        errorMessage = new StringWriter();
        when(hookResponse.err()).thenReturn(new PrintWriter(errorMessage));

    }

    @Test
    public void testOnReceive_repositoryHookConfigured(){
        when(repositoryHook.isConfigured()).thenReturn(true);
        when(repositoryHook.isEnabled()).thenReturn(true);

        RefChange refChange = mock(RefChange.class);
        boolean allowed = yaccPreReceiveHook.onReceive(repository, Lists.newArrayList(refChange), null);
        assertThat(allowed).isTrue();
    }

    @Test
    public void testOnReceive_deleteRefChangesIgnored() {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getType()).thenReturn(RefChangeType.DELETE);

        boolean allowed = yaccPreReceiveHook.onReceive(repository, Lists.newArrayList(refChange), null);
        assertThat(allowed).isTrue();
        verifyZeroInteractions(yaccService);
    }

    @Test
    public void testOnReceive_NullRefChangesIgnored() {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        when(refChange.getToHash()).thenReturn("0000000000000000000000000000000000000000");

        boolean allowed = yaccPreReceiveHook.onReceive(repository, Lists.newArrayList(refChange), null);
        assertThat(allowed).isTrue();
        verifyZeroInteractions(yaccService);
    }

    @Test
    public void testOnReceive_pushRejectedIfThereAreErrors() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error with commit")));

        boolean allowed = yaccPreReceiveHook.onReceive(repository, Lists.newArrayList(mock(RefChange.class)), hookResponse);
        assertThat(allowed).isFalse();
    }

    @Test
    public void testOnReceive_errorsArePrintedToHookStdErr() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1"), new YaccError("error2")));

        yaccPreReceiveHook.onReceive(repository, getMockRefChanges(), hookResponse);

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

        yaccPreReceiveHook.onReceive(repository, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString()).startsWith(YaccErrorBuilder.ERROR_BEARS);
    }

    @Test
    public void testOnReceive_nonEmptyErrorMessageHeaderReplacesDefaultHeader() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1")));

        when(settings.getString("errorMessageHeader")).thenReturn("Custom Header");

        yaccPreReceiveHook.onReceive(repository, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString()).isEqualTo("Custom Header\n" +
                "\n" +
                "refs/heads/master: error1\n\n");
    }

    @Test
    public void testOnReceive_errorMessageFooterAddedToEndOfOutput() {
        when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
                .thenReturn(Lists.newArrayList(new YaccError("error1")));

        when(settings.getString("errorMessageFooter")).thenReturn("Custom Footer");

        yaccPreReceiveHook.onReceive(repository, getMockRefChanges(), hookResponse);

        assertThat(errorMessage.toString()).endsWith("\nCustom Footer\n\n");
    }

    private List<RefChange> getMockRefChanges() {
        List<RefChange> refChanges = new ArrayList<RefChange>();
        RefChange refChange = mock(RefChange.class);

        when(refChange.getRefId()).thenReturn("refs/heads/master");

        refChanges.add(refChange);
        return refChanges;
    }

}
