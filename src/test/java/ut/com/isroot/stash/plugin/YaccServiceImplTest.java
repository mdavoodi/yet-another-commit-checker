package ut.com.isroot.stash.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.scm.git.command.GitCommand;
import com.atlassian.bitbucket.scm.git.command.GitRefCommandFactory;
import com.atlassian.bitbucket.scm.git.ref.GitAnnotatedTagCallback;
import com.atlassian.bitbucket.scm.git.ref.GitResolveAnnotatedTagsCommandParameters;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserType;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.TimeLimitedMatcherFactory;
import com.isroot.stash.plugin.YaccCommit;
import com.isroot.stash.plugin.YaccService;
import com.isroot.stash.plugin.YaccServiceImpl;
import com.isroot.stash.plugin.errors.YaccError;

import ut.com.isroot.stash.plugin.mock.MockGitAnnotatedTag;
import ut.com.isroot.stash.plugin.mock.MockRefChange;
import ut.com.isroot.stash.plugin.mock.MutableYaccSettings;

/**
 * @author Sean Ford
 * @since 2013-10-26
 */
public class YaccServiceImplTest {
    @Mock
    private AuthenticationContext stashAuthenticationContext;
    @Mock
    private GitRefCommandFactory gitRefCommandFactory;
    @Mock
    private JiraService jiraService;
    @Mock
    private ApplicationUser stashUser;

    private MutableYaccSettings settings;

    private YaccService yaccService;

    @Before
    public void setup() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");

        MockitoAnnotations.initMocks(this);
        settings = new MutableYaccSettings();

        yaccService = new YaccServiceImpl(stashAuthenticationContext, jiraService,
                gitRefCommandFactory);

        when(stashAuthenticationContext.getCurrentUser()).thenReturn(stashUser);
    }

    @Test
    public void testCheckCommit_rejectCatastrophicBacktrack() {
        settings.setCommitMessageRegex("^(a+)+$");

        YaccCommit commit = mockCommit();
        when(commit.getMessage())
                .thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaX");

        // commit should be rejected because regex is causing catastrophic backtrack and timeout is expired
        try {
            yaccService.checkCommit(settings, commit, null);
            failBecauseExceptionWasNotThrown(TimeLimitedMatcherFactory.RegExpTimeoutException.class);
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(TimeLimitedMatcherFactory.RegExpTimeoutException.class);
        }
    }


    @Test
    public void testCheckCommit_requireMatchingAuthorName_rejectOnMismatch() throws Exception {
        settings.setRequireMatchingAuthorName(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("Incorrect Name");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMITTER_NAME,
                "expected committer name 'John Smith' but found 'Incorrect Name'"));
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorName_allowOnMatch() throws Exception {
        settings.setRequireMatchingAuthorName(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("John Smith");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorName_notCaseSensitive() throws Exception {
        settings.setRequireMatchingAuthorName(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John SMITH");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("John Smith");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorName_crudIsIgnored() throws Exception {
        settings.setRequireMatchingAuthorName(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn(".,:;<>\"\\'John< >\nSMITH.,:;<>\"\\'");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("John Smith");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorEmail_rejectOnMismatch() throws Exception {
        settings.setRequireMatchingAuthorEmail(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMITTER_EMAIL,
                "expected committer email 'correct@email.com' but found 'wrong@email.com'"));
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorEmail_allowOnMatch() throws Exception {
        settings.setRequireMatchingAuthorEmail(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("correct@email.com");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorEmail_notCaseSensitive() throws Exception {
        settings.setRequireMatchingAuthorEmail(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("CoRrect@EMAIL.com");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorEmailRegex_rejectOnMismatch()
            throws Exception {
        settings.setRequireMatchingAuthorEmail(false);
        settings.setCommitterEmailRegex("correct@email.com");
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("wrong@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).contains(new YaccError(YaccError.Type.COMMITTER_EMAIL_REGEX,
                String.format("committer email regex '%s' does not match user email '%s'",
                        settings.getString("committerEmailRegex"),
                        commit.getCommitter().getEmailAddress())));
    }

    @Test
    public void testCheckCommit_requireMatchingAuthorEmailRegex_allowOnMatch() throws Exception {
        settings.setRequireMatchingAuthorEmail(false);
        settings.setCommitterEmailRegex(".*\\@email.com");

        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_serviceUser_skipped() {
        settings.setRequireMatchingAuthorName(true);
        settings.setRequireMatchingAuthorEmail(true);

        when(stashUser.getType()).thenReturn(UserType.SERVICE);

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("CoRrect@EMAIL.com");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
        verify(stashUser, never()).getDisplayName();
        verify(stashUser, never()).getEmailAddress();
    }

    @Test
    public void testCheckCommit_requireJiraIssue_rejectIfEnabledButNoJiraLinkExists()
            throws Exception {
        settings.setRequireJiraIssue(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(false);

        List<YaccError> errors = yaccService.checkCommit(settings, mockCommit(), null);
        assertThat(errors).containsOnly(new YaccError(
                "Unable to verify JIRA issue because JIRA Application Link does not exist"));
    }

    @Test
    public void testCheckCommit_requireJiraIssue_rejectIfNoJiraIssuesAreFound() {
        settings.setRequireJiraIssue(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn(
                "this commit message has no jira issues. abc-123 is not a valid issue because it is lowercase.");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.NO_JIRA_ISSUE,"No JIRA Issue found in commit message"));
    }

    @Test
    public void testCheckCommit_requireJiraIssue_ignoreUnknownJiraProjectKeys() throws Exception {
        settings.setRequireJiraIssue(true);
        settings.setIgnoreUnknownIssueProjectKeys(true);

        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("ABC", "123"))).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("UTF", "8"))).thenReturn(false);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn(
                "ABC-123: this commit has valid issue id and an invalid issue id of UTF-8");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
        verify(jiraService).doesJiraApplicationLinkExist();
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
        verify(jiraService).doesProjectExist(new IssueKey("ABC-123"));
        verify(jiraService).doesProjectExist(new IssueKey("UTF-8"));
    }

    @Test
    public void testCheckCommit_requireJiraIssue_rejectIfNoJiraIssuesWithAValidProjectAreFound()
            throws Exception {
        settings.setRequireJiraIssue(true);
        settings.setIgnoreUnknownIssueProjectKeys(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("UTF", "8"))).thenReturn(false);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn(
                "this commit message has no jira issues. UTF-8 is not a valid issue because it has an invalid project key.");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.NO_JIRA_ISSUE,"No JIRA Issue found in commit message"));
    }

    @Test
    public void testCheckCommit_requireJiraIssue_allowedIfValidJiraIssueIsFound() throws Exception {
        settings.setRequireJiraIssue(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
        verify(jiraService).doesJiraApplicationLinkExist();
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckCommit_requireJiraIssue_jiraIssueIdsAreExtractedFromCommitMessage()
            throws Exception {
        settings.setRequireJiraIssue(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage())
                .thenReturn("these issue ids should be extracted: ABC-123, ABC_D-123, ABC2-123");

        yaccService.checkCommit(settings, commit, null);
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC_D-123"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC2-123"));
    }

    @Test
    public void testCheckCommit_requireJiraIssue_errorsPassedThroughIfTheyAreReturned() {
        settings.setRequireJiraIssue(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class)))
                .thenReturn(Lists.newArrayList(new YaccError("some error")));

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).containsExactly(new YaccError("some error"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC", "123"));
    }

    @Test
    public void testCheckCommit_commitMessageRegex_commitMessageMatchesRegex() throws Exception {
        settings.setCommitMessageRegex("[a-z ]+");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("matches regex");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_commitMessageRegex_rejectIfCommitMessageDoesNotMatchRegex()
            throws Exception {
        settings.setCommitMessageRegex("[a-z ]+");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage())
                .thenReturn("123 does not match regex because it contains numbers");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMIT_REGEX,
                "commit message doesn't match regex: [a-z ]+"));
    }

    @Test
    public void testCheckCommit_excludeByRegex() {
        settings.setCommitMessageRegex("foo");

        // First verify that commit will be rejected
        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit will be rejected");

        assertThat(yaccService.checkCommit(settings, commit, null)).isNotEmpty();

        settings.setExcludeByRegex("#skipcheck");

        // Commit will be allowed if #skipcheck is present
        commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit will be allowed #skipcheck");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, null);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckCommit_excludeBranchRegex() {
        settings.setCommitMessageRegex("[A-Z0-9\\-]+: .*");
        settings.setExcludeBranchRegex("branch-name-excluded");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit should be rejected due to message regex");

        assertThat(yaccService.checkCommit(settings, commit, "branch-name")).isNotEmpty();

        assertThat(yaccService.checkCommit(settings, commit, "branch-name-excluded")).isEmpty();
    }

    @Test
    public void testCheckCommit_excludeBranchRegex_regexMustMatchFullBranchName() {
        settings.setCommitMessageRegex("[A-Z0-9\\-]+: .*");
        settings.setExcludeBranchRegex("[a-z]+");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("should be rejected");

        List<YaccError> errors = yaccService.checkCommit(settings, commit, "branch_0");
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMIT_REGEX,
                "commit message doesn't match regex: [A-Z0-9\\-]+: .*"));
    }

    @Test
    public void testCheckCommit_excludeMergeCommits() {
        settings.setCommitMessageRegex("foo");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("This is a merge commit");
        when(commit.isMerge()).thenReturn(true);

        // Should be rejected by default
        assertThat(yaccService.checkCommit(settings, commit, null)).isNotEmpty();

        settings.setExcludeMergeCommits(true);

        assertThat(yaccService.checkCommit(settings, commit, null)).isEmpty();
    }

    @Test
    public void testCheckCommit_excludeServiceUserCommits() {
        settings.setCommitMessageRegex("[a-z ]+");
        settings.setExcludeServiceUserCommits(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage())
                .thenReturn("123 does not match regex because it contains numbers");

        // normal user is rejected
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        assertThat(yaccService.checkCommit(settings, commit, null)).isNotEmpty();

        // service user is not
        when(stashUser.getType()).thenReturn(UserType.SERVICE);
        assertThat(yaccService.checkCommit(settings, commit, null)).isEmpty();
    }

    @Test
    public void testCheckCommit_excludeUsers() {
        settings.setCommitMessageRegex("[a-z ]+");
        settings.setExcludeUsers("excludeUser, anotherUser");

        YaccCommit commit = mockCommit();
        when(commit.getMessage())
                .thenReturn("123 does not match regex because it contains numbers");

        // commit rejected because user is not excluded
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getName()).thenReturn("notExcluded");
        assertThat(yaccService.checkCommit(settings, commit, null)).isNotEmpty();

        // commit allowed because user is not excluded
        when(stashUser.getName()).thenReturn("excludeUser");
        assertThat(yaccService.checkCommit(settings, commit, null)).isEmpty();
    }

    @Test
    public void testCheckRefChange_branchNameRegex_branchRejectedIfDoesNotMatchRegex() {
        settings.setBranchNameRegex("foo");

        RefChange refChange = mockRefAdd();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);

        assertThat(errors).containsOnly(new YaccError(YaccError.Type.BRANCH_NAME,
                "refs/heads/master: Invalid branch name. 'master' does not match regex 'foo'"));
    }

    @Test
    public void testCheckRefChange_branchNameRegex_branchAllowedIfItAlreadyExists() {
        settings.setBranchNameRegex("foo");

        RefChange refChange = mockRefChange();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);

        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckAnnotatedTag_normalUser_valid() {
        settings.setRequireMatchingAuthorName(true);
        settings.setRequireMatchingAuthorEmail(true);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        prepareGitRefCommandFactory(new MockGitAnnotatedTag("John Smith", "jsmith@example.com"));
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        RefChange refChange = mockRefAddTag();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckAnnotatedTag_normalUser_invalid() {
        settings.setRequireMatchingAuthorName(true);
        settings.setRequireMatchingAuthorEmail(true);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        prepareGitRefCommandFactory(
                new MockGitAnnotatedTag("Incorrect Name", "jsmith@example.com"));
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        RefChange refChange = mockRefAddTag();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMITTER_NAME,
                "refs/tags/v1.0.0: expected committer name 'John Smith' but found 'Incorrect Name'"));
    }

    @Test
    public void testCheckAnnotatedTag_serviceUser_skipped() {
        settings.setRequireMatchingAuthorName(true);
        settings.setRequireMatchingAuthorEmail(true);

        prepareGitRefCommandFactory(new MockGitAnnotatedTag("Access Key User", ""));
        when(stashUser.getType()).thenReturn(UserType.SERVICE);
        RefChange refChange = mockRefAddTag();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);
        assertThat(errors).isEmpty();
        verify(stashUser, never()).getDisplayName();
        verify(stashUser, never()).getEmailAddress();
    }

    private YaccCommit mockCommit() {
        YaccCommit commit = mock(YaccCommit.class, RETURNS_DEEP_STUBS);
        when(commit.getCommitter().getName()).thenReturn("John Smith");
        when(commit.getCommitter().getEmailAddress()).thenReturn("jsmith@example.com");
        when(commit.getId()).thenReturn("deadbeef");
        when(commit.isMerge()).thenReturn(false);
        return commit;
    }

    private MockRefChange mockRefAdd() {
        MockRefChange refChange = new MockRefChange();
        refChange.setFromHash("0000000000000000000000000000000000000000");
        refChange.setToHash("35d938b060bb361503e021f228e43351f1a71551");
        refChange.setRefId("refs/heads/master");
        refChange.setType(RefChangeType.ADD);
        return refChange;
    }

    private MockRefChange mockRefAddTag() {
        MockRefChange refChange = new MockRefChange();
        refChange.setFromHash("0000000000000000000000000000000000000000");
        refChange.setToHash("35d938b060bb361503e021f228e43351f1a71551");
        refChange.setRefId("refs/tags/v1.0.0");
        refChange.setType(RefChangeType.ADD);
        refChange.setRefType(StandardRefType.TAG);

        return refChange;
    }

    private MockRefChange mockRefChange() {
        MockRefChange refChange = new MockRefChange();
        refChange.setFromHash("5773fc438a763e64df8a9c5c32f3b1e83010ada7");
        refChange.setToHash("35d938b060bb361503e021f228e43351f1a71551");
        refChange.setRefId("refs/heads/master");
        refChange.setType(RefChangeType.UPDATE);
        return refChange;
    }

    private void prepareGitRefCommandFactory(MockGitAnnotatedTag gitAnnotatedTag) {
        when(gitRefCommandFactory.resolveAnnotatedTags(any(Repository.class),
                any(GitResolveAnnotatedTagsCommandParameters.class),
                any(GitAnnotatedTagCallback.class))).thenAnswer((Answer<?>) invocation -> {
                        GitAnnotatedTagCallback callback = invocation.getArgumentAt(2,
                            GitAnnotatedTagCallback.class);
                        callback.onTag(gitAnnotatedTag);

                        return mock(GitCommand.class);
                    });
    }
}