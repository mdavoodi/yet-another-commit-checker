package com.isroot.stash.plugin;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.isroot.stash.plugin.jira.JiraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author sdford
 * @since 2013-05-11
 */
public class ConfigValidator implements RepositorySettingsValidator
{
    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    private final JiraService jiraService;

    public ConfigValidator(JiraService jiraService)
    {
        this.jiraService = jiraService;
    }

    @Override
    public void validate(@Nonnull Settings settings, @Nonnull SettingsValidationErrors errors,
                         @Nonnull Repository repository)
    {
        validationRegex(settings, errors, "commitMessageRegex");
        validationRegex(settings, errors, "excludeByRegex");

        if(settings.getBoolean("requireJiraIssue", false))
        {
            if(!jiraService.doesJiraApplicationLinkExist())
            {
                errors.addFieldError("requireJiraIssue", "Can't be enabled because a JIRA application link does not exist.");
            }
        }

        String jqlMatcher = settings.getString("issueJqlMatcher");
        if(!isNullOrEmpty(jqlMatcher))
        {
            try
            {
                if(!jiraService.isJqlQueryValid(jqlMatcher))
                {
                    errors.addFieldError("issueJqlMatcher", "The JQL query syntax is invalid.");
                }
            }
            catch(ResponseException ex)
            {
                log.error("unexpected exception while trying to validate jql query", ex);
                errors.addFieldError("issueJqlMatcher", "Unable to validate JQL query with JIRA because there was an unexpected exception. Please see Stash logs.");
            }
            catch(CredentialsRequiredException ex)
            {
                log.error("authentication error while trying to validate jql query", ex);
                errors.addFieldError("issueJqlMatcher", "Unable to validate JQL query with JIRA. Authentication failure when communicating with JIRA.");
            }
        }
    }

    private void validationRegex(Settings settings,
                                 SettingsValidationErrors errors,
                                 String setting)
    {
        String regex = settings.getString(setting);
        if (regex != null && !regex.isEmpty())
        {
            try
            {
                Pattern.compile(regex);
            }
            catch (PatternSyntaxException ex)
            {
                errors.addFieldError(setting, "Invalid Regex: " + ex.getMessage());
            }
        }

    }
}
