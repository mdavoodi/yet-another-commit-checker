package ut.com.isroot.stash.plugin;

import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.testresources.pluginsettings.MockPluginSettings;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.YaccConfigServlet;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ut.com.isroot.stash.plugin.mock.MockSettingsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * @author jimb
 * @since 2015-08-17
 */
public class YaccConfigServletTest {

    @Mock private SoyTemplateRenderer soyTemplateRenderer;
    @Mock private PluginSettingsFactory pluginSettingsFactory;
    @Mock private JiraService jiraService;
    @Mock private RepositoryHookService repositoryHookService;
    @Mock private NavBuilder navBuilder;
    @Mock private NavBuilder.Addons addons;
    @Mock private Settings settings;
    @Mock private UserService userService;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private PluginSettings pluginSettings;

    private YaccConfigServlet yaccConfigServlet;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        pluginSettings = new MockPluginSettings();
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);

        when(repositoryHookService.createSettingsBuilder()).thenReturn(new MockSettingsBuilder());

        when(navBuilder.addons()).thenReturn(addons);
        when(addons.buildRelative()).thenReturn("/yaccHook/config");

        yaccConfigServlet = new YaccConfigServlet(soyTemplateRenderer,
                pluginSettingsFactory, jiraService, repositoryHookService, navBuilder, userService);
    }

    @Test
    public void testDoGet() throws IOException, ServletException, SoyException {
        yaccConfigServlet.doGet(request, response);
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("config", new HashMap());
        map.put("errors", new HashMap());
        verify(soyTemplateRenderer, times(1)).render(null,
                "com.isroot.stash.plugin.yacc:yaccHook-config-serverside",
                "com.atlassian.stash.repository.hook.ref.config", map);
    }

    @Test
    public void testDoPostNoParams() throws IOException, ServletException {
        yaccConfigServlet.doGet(request, response); // calling doGet to populate the settings map
        yaccConfigServlet.doPost(request, response);
        verify(response, times(1)).sendRedirect("/yaccHook/config");
    }

    @Test
    public void testDoPostWithParams() throws IOException, ServletException {
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("requireMatchingAuthorEmail", new String[]{"true"});
        parameterMap.put("requireMatchingAuthorName", new String[]{"true"});
        parameterMap.put("branchNameRegex", new String[]{"master"});
        when(request.getParameterMap()).thenReturn(parameterMap);
        when(request.getParameter(anyString())).then(invocationOnMock ->
                parameterMap.get(invocationOnMock.getArguments()[0].toString())[0]);

        yaccConfigServlet.doGet(request, response); // calling doGet to populate the settings map
        yaccConfigServlet.doPost(request, response);

        verify(response, times(1)).sendRedirect("/yaccHook/config");

        // verify settings were persisted
        Map<String, Object> savedSettings = (Map<String, Object>) pluginSettings.get(YaccConfigServlet.SETTINGS_MAP);

        Assertions.assertThat(savedSettings)
                .hasSize(3)
                .containsEntry("requireMatchingAuthorName", "true")
                .containsEntry("requireMatchingAuthorEmail", "true")
                .containsEntry("branchNameRegex", "master");
    }

}
