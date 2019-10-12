package com.isroot.stash.plugin;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookCommitFilter;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.StandardRepositoryHookTrigger;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.scm.git.command.GitRefCommandFactory;
import com.atlassian.bitbucket.scm.git.ref.GitAnnotatedTag;
import com.atlassian.bitbucket.scm.git.ref.GitAnnotatedTagCallback;
import com.atlassian.bitbucket.scm.git.ref.GitResolveAnnotatedTagsCommandParameters;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserType;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.checks.BranchNameCheck;
import com.isroot.stash.plugin.errors.YaccError;
import com.isroot.stash.plugin.errors.YaccErrorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    private final JiraService jiraService;
    private final GitRefCommandFactory gitRefCommandFactory;

    public YaccServiceImpl(AuthenticationContext stashAuthenticationContext,
            JiraService jiraService, GitRefCommandFactory gitRefCommandFactory) {
        this.stashAuthenticationContext = stashAuthenticationContext;
        this.jiraService = jiraService;
        this.gitRefCommandFactory = gitRefCommandFactory;
    }

    @Override
    public RepositoryHookResult check(PreRepositoryHookContext context,
                                      RepositoryHookRequest repositoryHookRequest,
                                      Settings settings) {
        log.debug("YaccHook preUpdate, registering commit callback. settings={}", settings);

        Repository repository = repositoryHookRequest.getRepository();

        List<YaccError> errors = checkRefs(repository, settings,
                repositoryHookRequest.getRefChanges());
        if (!errors.isEmpty()) {
            YaccErrorBuilder errorBuilder = new YaccErrorBuilder(settings);
            String message = errorBuilder.getErrorMessage(errors);

            log.debug("push rejected by ref checks, errors={}", errors);

            return RepositoryHookResult.rejected("Push rejected by YACC", message);
        }

        YaccHookCommitCallback callback = new YaccHookCommitCallback(this, settings,
                showDefaultRejectMessageHeader(repositoryHookRequest));

        context.registerCommitCallback(callback, RepositoryHookCommitFilter.ADDED_TO_REPOSITORY);

        // Will be accepted unless commit callback rejects a commit
        return RepositoryHookResult.accepted();
    }

    private boolean showDefaultRejectMessageHeader(RepositoryHookRequest repositoryHookRequest) {
        return repositoryHookRequest.getTrigger() != StandardRepositoryHookTrigger.FILE_EDIT;
    }

    private List<YaccError> checkRefs(Repository repository, Settings settings,
            Collection<RefChange> refChanges) {
        List<YaccError> errors = new ArrayList<>();

        for (RefChange refChange : refChanges) {
            log.debug("refChange: ref={} refType={} type={} fromHash={} toHash={}",
                    refChange.getRef(), refChange.getRef().getType(), refChange.getType(),
                    refChange.getFromHash(), refChange.getToHash());

            errors = checkRefChange(repository, settings, refChange);
        }

        return errors;
    }

    @Override
    public List<YaccError> checkRefChange(Repository repository, Settings settings, RefChange refChange) {
        List<YaccError> errors = new ArrayList<>();

        if (refChange.getType() == RefChangeType.ADD) {
            errors.addAll(new BranchNameCheck(settings, refChange.getRef().getId()).check());

            if (refChange.getRef().getType() == StandardRefType.TAG) {
                errors.addAll(checkAnnotatedTag(repository, settings, refChange));
            }
        }

        List<YaccError> errorsWithRef = new ArrayList<>();
        for (YaccError e : errors) {
            errorsWithRef.add(e.prependText(refChange.getRef().getId()));
        }

        return errorsWithRef;
    }

    @Override
    public List<YaccError> checkCommit(Settings settings, YaccCommit commit, String branchName) {
        log.debug("checking commit id={} name={} email={} message={} branchName={}", commit.getId(),
                commit.getCommitter().getName(), commit.getCommitter().getEmailAddress(),
                commit.getMessage(), branchName);

        List<YaccError> errors = Lists.newArrayList();

        errors.addAll(checkCommitter(settings, commit));

        if (!isCommitExcluded(settings, commit) && !isBranchExcluded(settings, branchName)) {
            errors.addAll(checkCommitMessageRegex(settings, commit));

            // Checking JIRA issues might be dependent on the commit message regex, so only proceed if there are no errors.
            if (errors.isEmpty()) {
                errors.addAll(checkJiraIssues(settings, commit));
            }
        }

        return errors;
    }

    private List<YaccError> checkAnnotatedTag(Repository repository, Settings settings,
            RefChange refChange) {
        List<YaccError> errors = new ArrayList<>();

        log.info("checking annotated tags");

        GitResolveAnnotatedTagsCommandParameters params =
                new GitResolveAnnotatedTagsCommandParameters.Builder()
                        .tagIds(refChange.getToHash())
                        .build();

        GitAnnotatedTagCallback callback = new GitAnnotatedTagCallback() {
            @Override
            public boolean onTag(@Nonnull GitAnnotatedTag gitAnnotatedTag) throws IOException {
                log.info("tag refId={} tagger={}", gitAnnotatedTag.getId(),
                        gitAnnotatedTag.getTagger());

                YaccCommit yaccCommit = new YaccCommit(gitAnnotatedTag);

                errors.addAll(checkCommitter(settings, yaccCommit));

                return true;
            }
        };

        gitRefCommandFactory.resolveAnnotatedTags(repository, params, callback)
                .call();

        return errors;
    }

    private boolean isCommitExcluded(Settings settings, YaccCommit commit) {
        // Exclude Merge Commit setting
        if (settings.getBoolean("excludeMergeCommits", false) && commit.isMerge()) {
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

        if (excludeRegex != null && !excludeRegex.isEmpty()) {
            Pattern pattern = Pattern.compile(excludeRegex);
            Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, commit.getMessage());
            if (matcher.find()) {
                log.debug("commit excluded because excludeByRegex={} matches", excludeRegex);
                return true;
            }
        }

        return false;
    }

    private boolean isBranchExcluded(Settings settings, String branchName) {
        // Exclude by Regex setting
        String excludeBranchRegex = settings.getString("excludeBranchRegex");

        log.debug("branch check, excludeBranchRegex={} branchName={}", excludeBranchRegex,
                branchName);

        if (excludeBranchRegex != null && !excludeBranchRegex.isEmpty()) {
            Pattern pattern = Pattern.compile(excludeBranchRegex);
            Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, branchName);
            if (matcher.matches()) {
                log.debug("branch is excluded");
                return true;
            }
        }

        return false;
    }

    private List<YaccError> checkCommitMessageRegex(Settings settings, YaccCommit commit) {
        List<YaccError> errors = Lists.newArrayList();

        String regex = settings.getString("commitMessageRegex");
        if (!isNullOrEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, commit.getMessage());
            if (!matcher.matches()) {
                errors.add(new YaccError(YaccError.Type.COMMIT_REGEX,
                        "commit message doesn't match regex: " + regex));
            }
        }

        return errors;
    }

    private List<YaccError> checkCommitterEmailRegex(Settings settings, YaccCommit commit) {
        List<YaccError> errors = Lists.newArrayList();
        String regex = settings.getString("committerEmailRegex");
        if (!isNullOrEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, commit.getCommitter().getEmailAddress().toLowerCase());
            if (!matcher.matches()) {
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
        if (!isNullOrEmpty(regex)) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher =  TimeLimitedMatcherFactory.matcher(pattern, message);
            if (matcher.matches() && matcher.groupCount() > 0) {
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

        final boolean ignoreUnknownProjectKeys = settings
                .getBoolean("ignoreUnknownIssueProjectKeys", false);

        log.debug("ignoreUnknownIssueProjectKeys={}", ignoreUnknownProjectKeys);

        if (ignoreUnknownProjectKeys) {
            /* Remove issues that contain non-existent project keys */
            issues = Lists.newArrayList();
            for (IssueKey issueKey : extractedKeys) {
                if (jiraService.doesProjectExist(issueKey)) {
                    issues.add(issueKey);
                }
            }
        } else {
            issues = extractedKeys;
        }

        if (!issues.isEmpty()) {
            for (IssueKey issueKey : issues) {
                errors.addAll(checkJiraIssue(settings, issueKey));
            }
        } else {
            errors.add(new YaccError(YaccError.Type.NO_JIRA_ISSUE, "No JIRA Issue found in commit message"));
        }

        return errors;
    }

    private List<YaccError> checkJiraIssue(Settings settings, IssueKey issueKey) {
        List<YaccError> errors = Lists.newArrayList();

        log.debug("checking JIRA issue={}", issueKey);

        errors.addAll(jiraService.doesIssueExist(issueKey));

        if (errors.isEmpty()) {
            String jqlQuery = settings.getString("issueJqlMatcher");

            if (jqlQuery != null && !jqlQuery.isEmpty()) {
                errors.addAll(jiraService.doesIssueMatchJqlQuery(jqlQuery, issueKey));
            }
        }

        return errors;
    }

    private List<YaccError> checkCommitter(Settings settings, YaccCommit commit) {
        List<YaccError> errors = new ArrayList<>();

        ApplicationUser stashUser = stashAuthenticationContext.getCurrentUser();

        if (stashUser == null) {
            // This should never happen. User returned by getCurrentUser is
            // marked as nullable though.
            log.warn("Unauthenticated user is committing - skipping committer validate checks");
        } else {
            // Only validate 'normal' users - service users like
            // the ssh access keys use the key comment as the 'name' and don't have emails
            // Neither of these are useful to validate, so just skip them
            if (stashUser.getType() == UserType.NORMAL) {
                errors.addAll(checkCommitterEmail(settings, commit, stashUser));
                errors.addAll(checkCommitterName(settings, commit, stashUser));
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
     * <p>
     * See strbuf_addstr_without_crud() in git's ident.c.
     * Link: https://github.com/git/git/blob/master/ident.c#L155 (current as of 2014-10-06).
     */
    private String removeGitCrud(String name) {
        if (name != null) {
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
