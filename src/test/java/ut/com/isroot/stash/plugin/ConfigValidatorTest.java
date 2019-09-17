package ut.com.isroot.stash.plugin;

import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.isroot.stash.plugin.ConfigValidator;
import com.isroot.stash.plugin.JiraService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2014-01-13
 */
public class ConfigValidatorTest {
    @Mock private JiraService jiraService;
    @Mock private Settings settings;
    @Mock private SettingsValidationErrors settingsValidationErrors;
    @Mock private Scope scope;

    private ConfigValidator configValidator;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        System.setProperty("line.separator", "\n");

        configValidator = new ConfigValidator(jiraService);
    }

    @Test
    public void testValidate_errorIfRequireJiraIssueIsOnAndNoAppLink() {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(false);

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settingsValidationErrors).addFieldError("requireJiraIssue", "Can't be enabled because a JIRA application link does not exist.");
    }

    @Test
    public void testValidate_jqlQueryErrorsAreAddedToValidationErrors() {
        when(settings.getString("issueJqlMatcher")).thenReturn("this jql query is invalid");

        List<String> errors = new ArrayList<>();
        errors.add("some error");
        when(jiraService.checkJqlQuery(anyString())).thenReturn(errors);

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).checkJqlQuery("this jql query is invalid");
        verify(settingsValidationErrors).addFieldError("issueJqlMatcher", "some error");
    }

    @Test
    public void testValidate_validJqlQueryIsAccepted() {
        when(settings.getString("issueJqlMatcher")).thenReturn("assignee is not empty");
        when(jiraService.checkJqlQuery(anyString())).thenReturn(new ArrayList<>());

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("issueJqlMatcher");
        verify(jiraService).checkJqlQuery("assignee is not empty");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_excludeByRegex_emptyStringAllowed() {
        when(settings.getString("excludeByRegex")).thenReturn("");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("excludeByRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_excludeByRegex_goodRegex() {
        when(settings.getString("excludeByRegex")).thenReturn("^Revert \"|#skipchecks");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("excludeByRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }
    @Test
    public void testValidate_excludeByRegex_badRegex() {
        when(settings.getString("excludeByRegex")).thenReturn("^(");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("excludeByRegex");
        verify(settingsValidationErrors).addFieldError("excludeByRegex", "Invalid Regex: Unclosed group near index 2\n" +
                "^(");
    }

    @Test
    public void testValidate_excludeBranchRegex_emptyStringAllowed() {
        when(settings.getString("excludeBranchRegex")).thenReturn("");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("excludeBranchRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_excludeBranchRegex_goodRegex() {
        when(settings.getString("excludeBranchRegex")).thenReturn("valid regex");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("excludeBranchRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }
    @Test
    public void testValidate_excludeBranchRegex_badRegex() {
        when(settings.getString("excludeBranchRegex")).thenReturn("^(");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("excludeBranchRegex");
        verify(settingsValidationErrors).addFieldError("excludeBranchRegex", "Invalid Regex: Unclosed group near index 2\n" +
                "^(");
    }

    @Test
    public void testValidate_commitMessageRegex_emptyStringAllowed() {
        when(settings.getString("commitMessageRegex")).thenReturn("");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("commitMessageRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_commitMessageRegex_goodRegex() {
        when(settings.getString("commitMessageRegex")).thenReturn(".{32,}");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("commitMessageRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_commitMessageRegex_badRegex() {
        when(settings.getString("commitMessageRegex")).thenReturn(")");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("commitMessageRegex");
        verify(settingsValidationErrors).addFieldError("commitMessageRegex", "Invalid Regex: Unmatched closing ')'\n" +
                ")");
    }

    @Test
    public void testValidate_branchNameRegex_goodRegex() {
        when(settings.getString("branchNameRegex")).thenReturn("feature/[A-Z]+-\\d+-[A-Z-]*");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("branchNameRegex");
        verifyZeroInteractions(settingsValidationErrors);
    }

    @Test
    public void testValidate_branchNameRegex_badRegex() {
        when(settings.getString("branchNameRegex")).thenReturn(")");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settings).getString("branchNameRegex");
        verify(settingsValidationErrors).addFieldError("branchNameRegex", "Invalid Regex: Unmatched closing ')'\n" +
                ")");
    }

    @Test
    public void testValidate_committerEmailRegex_isValidated() {
        when(settings.getString("committerEmailRegex")).thenReturn(")");

        configValidator.validate(settings, settingsValidationErrors, scope);

        verify(settingsValidationErrors)
                .addFieldError("committerEmailRegex", "Invalid Regex: Unmatched closing ')'\n)");
    }
}
