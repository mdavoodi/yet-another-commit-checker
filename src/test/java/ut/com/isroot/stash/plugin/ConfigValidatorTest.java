package ut.com.isroot.stash.plugin;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.isroot.stash.plugin.ConfigValidator;
import com.isroot.stash.plugin.jira.JiraService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
    public void testValidate_authenticationErrorWhenValidatingQueryReturnsError() throws Exception
    {
        when(settings.getString("issueJqlMatcher")).thenReturn("assignee is not empty");
        when(jiraService.isJqlQueryValid(anyString())).thenThrow(CredentialsRequiredException.class);

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).isJqlQueryValid("assignee is not empty");
        verify(settingsValidationErrors).addFieldError("issueJqlMatcher", "Unable to validate JQL query with JIRA. Authentication failure when communicating with JIRA.");
    }

    @Test
    public void testValidate_invalidJqlQueryAddsValidationError() throws Exception
    {
        when(settings.getString("issueJqlMatcher")).thenReturn("this jql query is invalid");
        when(jiraService.isJqlQueryValid(anyString())).thenReturn(false);

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).isJqlQueryValid("this jql query is invalid");
        verify(settingsValidationErrors).addFieldError("issueJqlMatcher", "The JQL query syntax is invalid.");
    }

    @Test
    public void testValidate_validJqlQueryIsAccepted() throws Exception
    {
        when(settings.getString("issueJqlMatcher")).thenReturn("assignee is not empty");
        when(jiraService.isJqlQueryValid(anyString())).thenReturn(true);

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).isJqlQueryValid("assignee is not empty");
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

    @Test
    public void testValidate_messagesLang_ok() {
        when(settings.getString("messagesLang")).thenReturn("en");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("messagesLang");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_messagesLang_error() {
        when(settings.getString("messagesLang")).thenReturn("invalid_lang");

        configValidator.validate(settings, settingsValidationErrors, repository);

        verify(settings).getString("messagesLang");
        verify(settingsValidationErrors).addFieldError("messagesLang", "Language code is invalid or not supported: Couldn't find 3-letter language code for invalid_lang");
    }

}
