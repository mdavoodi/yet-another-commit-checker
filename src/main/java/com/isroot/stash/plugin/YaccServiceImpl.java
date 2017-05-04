package com.isroot.stash.plugin;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.git.GitRefPattern;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserType;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.checks.BranchNameCheck;
import com.isroot.stash.plugin.commits.CommitsService;
import com.isroot.stash.plugin.errors.YaccError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author Sean Ford
 * @since 2014-01-14
 */
public class YaccServiceImpl implements YaccService {
    private static final Logger log = LoggerFactory.getLogger(YaccServiceImpl.class);


    private final AuthenticationContext stashAuthenticationContext;
    private final CommitsService commitsService;
    private final JiraService jiraService;

    public YaccServiceImpl(AuthenticationContext stashAuthenticationContext, CommitsService commitsService,
                           JiraService jiraService) {
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.commitsService = commitsService;
        this.jiraService = jiraService;
    }

    @Override
    public List<YaccError> checkRefChange(Repository repository, Settings settings, RefChange refChange) {
        boolean isTag = refChange.getRef().getId().startsWith(GitRefPattern.TAGS.getPath());

        List<YaccError> errors = Lists.newArrayList();

        if (refChange.getType() == RefChangeType.ADD) {
            errors.addAll(new BranchNameCheck(settings, refChange.getRef().getId()).check());
        }

        Set<YaccCommit> commits = commitsService.getNewCommits(repository, refChange);
        String branchName = refChange.getRef().getId().replace(GitRefPattern.HEADS.getPath(), "");
        
        for (YaccCommit commit : commits) {
            for(YaccError e : checkCommit(settings, commit, !isTag, branchName)) {
                errors.add(e.prependText(commit.getId()));
            }
        }

        return errors;
    }

    private List<YaccError> checkCommit(Settings settings, YaccCommit commit, boolean checkMessages, String branchName) {
        log.debug("checking commit id={} name={} email={} message={}", commit.getId(),
                commit.getCommitter().getName(), commit.getCommitter().getEmailAddress(),
                commit.getMessage());

        List<YaccError> errors = Lists.newArrayList();

        ApplicationUser stashUser = stashAuthenticationContext.getCurrentUser();

        if (stashUser == null) {
            // This should never happen, so skip the checks.
            log.warn("Unauthenticated user is committing - skipping committer validate checks");
            return errors;
        }

        //Skip all checks if the commit or branch are excluded.
        if(!isCommitExcluded(settings, commit) && !isBranchExcluded(settings, branchName)) {
        
            // Only validate email/name for 'normal' users - service users like
            // the ssh access keys use the key comment as the 'name' and don't have emails
            // Neither of these are useful to validate, so just skip them
            if (stashUser.getType() == UserType.NORMAL) {
                errors.addAll(checkCommitterEmail(settings, commit, stashUser));
                errors.addAll(checkCommitterName(settings, commit, stashUser));
            }
        
            if(checkMessages) {
                errors.addAll(checkCommitMessageRegex(settings, commit));
                
                // Checking JIRA issues might be dependent on the commit message regex, so only proceed if there are no errors.
                if (errors.isEmpty()) {
                    errors.addAll(checkJiraIssues(settings, commit));
                }
            }
        }

        return errors;
    }

    private boolean isCommitExcluded(Settings settings, YaccCommit commit) {
        // Exclude Merge Commit setting
        if(settings.getBoolean("excludeMergeCommits", false) && commit.isMerge()) {
            log.debug("skipping commit {} because it is a merge commit", commit.getId());

            return true;
        }

        // Exclude by Service User setting
        ApplicationUser stashUser = stashAuthenticationContext.getCurrentUser();
        if (settings.getBoolean("excludeServiceUserCommits", false) && stashUser.getType() == UserType.SERVICE) {
            return true;
        }

        // Exclude by User setting
        if (stashUser.getType() == UserType.NORMAL) {
            String excludeUsers = settings.getString("excludeUsers");
            if (excludeUsers != null) {
                List<String> excludedUsers = Arrays.asList((excludeUsers.split(","))).stream().map(i -> i.trim()).collect(Collectors.toList());

                log.debug("checking exclude users setting for user {}: {}", stashUser.getName(),
                        excludedUsers);

                if (excludedUsers.contains(stashUser.getName())) {
                    log.debug("commit excluded due to exclude users setting");
                    return true;
                }
            }
        }

        // Exclude by Regex setting
        String excludeRegex = settings.getString("excludeByRegex");

        if(excludeRegex != null && !excludeRegex.isEmpty()) {
            Pattern pattern = Pattern.compile(excludeRegex);
            Matcher matcher = pattern.matcher(commit.getMessage());
            if(matcher.find()) {
                return true;
            }
        }

        return false;
    }
    
    private boolean isBranchExcluded(Settings settings, String branchName) {
        // Exclude by Regex setting
        String excludeBranchRegex = settings.getString("excludeBranchRegex");

        if(excludeBranchRegex != null && !excludeBranchRegex.isEmpty()) {
            Pattern pattern = Pattern.compile(excludeBranchRegex);
            Matcher matcher = pattern.matcher(branchName);
            if(matcher.matches()) {
                return true;
            }
        }

        return false;
    }

    private List<YaccError> checkCommitMessageRegex(Settings settings, YaccCommit commit) {
        List<YaccError> errors = Lists.newArrayList();

        String regex = settings.getString("commitMessageRegex");
        if(!isNullOrEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(commit.getMessage());
            if(!matcher.matches()) {
                errors.add(new YaccError(YaccError.Type.COMMIT_REGEX,
                        "commit message doesn't match regex: " + regex));
            }
        }

        return errors;
    }

    private List<YaccError> checkCommitterEmailRegex(Settings settings, YaccCommit commit) {
        List<YaccError> errors = Lists.newArrayList();
        String regex = settings.getString("committerEmailRegex");
        if(!isNullOrEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(commit.getCommitter().getEmailAddress().toLowerCase());
            if(!matcher.matches()) {
                errors.add(new YaccError(YaccError.Type.COMMITTER_EMAIL_REGEX,
                    String.format("committer email regex '%s' does not match user email '%s'", regex,
                    commit.getCommitter().getEmailAddress())));
            }
        }

        return errors;
    }
    private List<IssueKey> extractJiraIssuesFromCommitMessage(Settings settings, YaccCommit commit) {
        String message = commit.getMessage();

        // If a commit message regex is present, see if it contains a group 1 that can be used to located JIRA issues.
        // If not, just ignore it.
        String regex = settings.getString("commitMessageRegex");
        if(!isNullOrEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(message);
            if(matcher.matches() && matcher.groupCount() > 0) {
                message = matcher.group(1);
            }
        }

        final List<IssueKey> issueKeys = IssueKey.parseIssueKeys(message);
        log.debug("found jira issues {} from commit message: {}", issueKeys, message);

        return issueKeys;
    }

    private List<YaccError> checkJiraIssues(Settings settings, YaccCommit commit) {
        if (!settings.getBoolean("requireJiraIssue", false)) {
            return Lists.newArrayList();
        }

        List<YaccError> errors = Lists.newArrayList();

        if (!jiraService.doesJiraApplicationLinkExist()) {
            errors.add(new YaccError("Unable to verify JIRA issue because JIRA Application Link does not exist"));
            return errors;
        }

        final List<IssueKey> issues;
        final List<IssueKey> extractedKeys = extractJiraIssuesFromCommitMessage(settings, commit);
        if (settings.getBoolean("ignoreUnknownIssueProjectKeys", false)) {
            /* Remove issues that contain non-existent project keys */
            issues = Lists.newArrayList();
            for (IssueKey issueKey : extractedKeys) {
                if (jiraService.doesProjectExist(issueKey)) {
                    issues.add(issueKey);
                }
            }
        }
        else {
            issues = extractedKeys;
        }

        if(!issues.isEmpty()) {
            for(IssueKey issueKey : issues) {
                errors.addAll(checkJiraIssue(settings, issueKey));
            }
        }
        else {
            errors.add(new YaccError("No JIRA Issue found in commit message."));
        }

        return errors;
    }

    private List<YaccError> checkJiraIssue(Settings settings, IssueKey issueKey) {
        List<YaccError> errors = Lists.newArrayList();

        errors.addAll(jiraService.doesIssueExist(issueKey));

        if(errors.isEmpty()) {
            String jqlQuery = settings.getString("issueJqlMatcher");

            if (jqlQuery != null && !jqlQuery.isEmpty()) {
                errors.addAll(jiraService.doesIssueMatchJqlQuery(jqlQuery, issueKey));
            }
        }

        return errors;
    }

    private List<YaccError> checkCommitterEmail(@Nonnull Settings settings, @Nonnull YaccCommit commit, @Nonnull ApplicationUser stashUser) {
        final boolean requireMatchingAuthorEmail = settings.getBoolean("requireMatchingAuthorEmail", false);
        List<YaccError> errors = Lists.newArrayList();

        // while the email address is not marked as @Nullable, its not @Notnull either
        // For service users it can be null, and while those have already been
        // excluded, add a sanity check anyway

        if (stashUser.getEmailAddress() == null) {
            log.warn("stash user has null email address - skipping email validation");
            return errors;
        }

        log.debug("requireMatchingAuthorEmail={} authorEmail={} stashEmail={}", requireMatchingAuthorEmail, commit.getCommitter().getEmailAddress(),
                stashUser.getEmailAddress());

        if (requireMatchingAuthorEmail && !commit.getCommitter().getEmailAddress().toLowerCase().equals(stashUser.getEmailAddress().toLowerCase())) {
            errors.add(new YaccError(YaccError.Type.COMMITTER_EMAIL,
                    String.format("expected committer email '%s' but found '%s'", stashUser.getEmailAddress(),
                    commit.getCommitter().getEmailAddress())));
        }

        errors.addAll(checkCommitterEmailRegex(settings, commit));
        return errors;
    }

    private List<YaccError> checkCommitterName(@Nonnull Settings settings, @Nonnull YaccCommit commit, @Nonnull ApplicationUser stashUser) {
        final boolean requireMatchingAuthorName = settings.getBoolean("requireMatchingAuthorName", false);

        List<YaccError> errors = Lists.newArrayList();

        log.debug("requireMatchingAuthorName={} authorName={} stashName={}", requireMatchingAuthorName, commit.getCommitter().getName(),
                stashUser.getDisplayName());

        String name = removeGitCrud(stashUser.getDisplayName());

        if (requireMatchingAuthorName && !commit.getCommitter().getName().equalsIgnoreCase(name)) {
            errors.add(new YaccError(YaccError.Type.COMMITTER_NAME,
                    String.format("expected committer name '%s' but found '%s'", name,
                    commit.getCommitter().getName())));
        }

        return errors;
    }

    /**
     * Remove special characters and "crud" from name. This works around a git issue where it
     * allows these characters in user.name but will strip them out when doing a commit. Leaving
     * these characters breaks YACC name matching because Stash will provide the Stash user's name
     * with these characters, however, they will never appear in the commit so author name will
     * never match.
     *
     * See strbuf_addstr_without_crud() in git's ident.c.
     * Link: https://github.com/git/git/blob/master/ident.c#L155 (current as of 2014-10-06).
     */
    private String removeGitCrud(String name) {
        if(name != null) {
            // remove special characters
            name = name.replaceAll("[<>\n]", "");

            // remove leading crud
            name = name.replaceAll("^[\\\\.,:;\"']*", "");

            // remove trailing crud
            name = name.replaceAll("[\\\\.,:;\"']*$", "");
        }

        return name;
    }
}
