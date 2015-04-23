package ut.com.isroot.stash.plugin;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.google.common.collect.ImmutableList;
import com.isroot.stash.plugin.ConfigValidator;
import com.isroot.stash.plugin.JiraLookupsException;
import com.isroot.stash.plugin.JiraService;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 * @author Sean Ford
 * @since 2014-01-13
 */
public class ConfigValidatorTest
{
    @Mock private JiraService jiraService;
    @Mock private Settings settings;
    @Mock private SettingsValidationErrors settingsValidationErrors;
    @Mock private Repository repository;


    private ConfigValidator configValidator;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        configValidator = new ConfigValidator(jiraService);
    }

    @Test
    public void testValidate_errorIfRequireJiraIssueIsOnAndNoAppLink()
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(false);

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settingsValidationErrors).addFieldError("requireJiraIssue", "Can't be enabled because a JIRA application link does not exist.");
    }

    @Test
    public void testValidate_ErrorWhenValidatingQueryReturnsError() throws Exception
    {
        when(settings.getString("issueJqlMatcher")).thenReturn("assignee is not empty");

        when(jiraService.checkJqlQuery(anyString())).thenReturn(ImmutableList.of("MOCK ERROR"));

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).checkJqlQuery("assignee is not empty");
        verify(settingsValidationErrors).addFieldError("issueJqlMatcher", "MOCK ERROR");
    }

    @Test
    public void testValidate_invalidJqlQueryAddsValidationError() throws Exception
    {
        when(settings.getString("issueJqlMatcher")).thenReturn("this jql query is invalid");
        when(jiraService.checkJqlQuery(anyString())).thenReturn(ImmutableList.of("INVALID"));

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).checkJqlQuery("this jql query is invalid");
        verify(settingsValidationErrors).addFieldError("issueJqlMatcher", "INVALID");
    }

    @Test
    public void testValidate_validJqlQueryIsAccepted() throws Exception
    {
        when(settings.getString("issueJqlMatcher")).thenReturn("assignee is not empty");
        when(jiraService.checkJqlQuery(anyString())).thenReturn(ImmutableList.<String>of());

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).checkJqlQuery("assignee is not empty");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_excludeByRegex_emptyStringAllowed()
    {
        when(settings.getString("excludeByRegex")).thenReturn("");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("excludeByRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_excludeByRegex_goodRegex()
    {
        when(settings.getString("excludeByRegex")).thenReturn("^Revert \"|#skipchecks");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("excludeByRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }
    @Test
    public void testValidate_excludeByRegex_badRegex()
    {
        when(settings.getString("excludeByRegex")).thenReturn("^(");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("excludeByRegex");
        verify(settingsValidationErrors).addFieldError("excludeByRegex", "Invalid Regex: Unclosed group near index 2\n" +
                "^(\n" +
                "  ^");
    }

    @Test
    public void testValidate_commitMessageRegex_emptyStringAllowed()
    {
        when(settings.getString("commitMessageRegex")).thenReturn("");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("commitMessageRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_commitMessageRegex_goodRegex()
    {
        when(settings.getString("commitMessageRegex")).thenReturn(".{32,}");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("commitMessageRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }
    @Test
    public void testValidate_commitMessageRegex_badRegex()
    {
        when(settings.getString("commitMessageRegex")).thenReturn(")");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("commitMessageRegex");
        verify(settingsValidationErrors).addFieldError("commitMessageRegex", "Invalid Regex: Unmatched closing ')'\n" +
                ")");
    }

}
