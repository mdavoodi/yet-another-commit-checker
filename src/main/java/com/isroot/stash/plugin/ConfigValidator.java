package com.isroot.stash.plugin;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import static com.google.common.base.Strings.isNullOrEmpty;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            List<String> jqlErrors = jiraService.checkJqlQuery(jqlMatcher);
            for (String err : jqlErrors)
            {
                errors.addFieldError("issueJqlMatcher", err);
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
