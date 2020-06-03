package com.isroot.stash.plugin;

import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import com.atlassian.bitbucket.user.UserService;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author sdford
 * @since 2013-05-11
 */
public class ConfigValidator implements SettingsValidator {
    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    private final JiraService jiraService;
    private final UserService userService;

    public ConfigValidator(JiraService jiraService, UserService userService) {
        this.jiraService = jiraService;
        this.userService = userService;
    }

    @Override
    public void validate(@Nonnull Settings settings, @Nonnull SettingsValidationErrors errors,
                         @Nonnull Scope scope) {
        log.debug("validating config");

        validateImpersonationUser(settings, errors);

        validationRegex(settings, errors, "commitMessageRegex");
        validationRegex(settings, errors, "committerEmailRegex");
        validationRegex(settings, errors, "excludeByRegex");
        validationRegex(settings, errors, "excludeBranchRegex");
        validationRegex(settings, errors, "branchNameRegex");

        if (settings.getBoolean("requireJiraIssue", false)) {
            if (!jiraService.doesJiraApplicationLinkExist()) {
                errors.addFieldError("requireJiraIssue", "Can't be enabled because a JIRA application link does not exist.");
            }
        }

        String jqlMatcher = settings.getString("issueJqlMatcher");
        if (!isNullOrEmpty(jqlMatcher)) {
            List<String> jqlErrors = jiraService.checkJqlQuery(jqlMatcher);
            for (String err : jqlErrors) {
                errors.addFieldError("issueJqlMatcher", err);
            }
        }
    }

    private void validationRegex(Settings settings,
                                 SettingsValidationErrors errors,
                                 String setting) {
        String regex = settings.getString(setting);
        if (regex != null && !regex.isEmpty()) {
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException ex) {
                errors.addFieldError(setting, "Invalid Regex: " + ex.getMessage());
            }
        }

    }

    private void validateImpersonationUser(Settings settings, SettingsValidationErrors errors) {
        Optional.ofNullable(settings.getString("overrideJiraUser")).ifPresent(userName ->
            {
                if (!Strings.isNullOrEmpty(userName) && userService.getUserByName(userName) == null) {
                    errors.addFieldError("overrideJiraUser", "User does not exist.");
                }
            }
        );
    }
}
