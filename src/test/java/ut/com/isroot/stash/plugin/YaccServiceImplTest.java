package ut.com.isroot.stash.plugin;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.isroot.stash.plugin.commits.CommitsService;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.YaccCommit;
import com.isroot.stash.plugin.YaccService;
import com.isroot.stash.plugin.YaccServiceImpl;
import com.isroot.stash.plugin.errors.YaccError;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ut.com.isroot.stash.plugin.mock.MockRefChange;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2013-10-26
 */
public class YaccServiceImplTest {
    @Mock private AuthenticationContext stashAuthenticationContext;
    @Mock private CommitsService commitsService;
    @Mock private JiraService jiraService;
    @Mock private Settings settings;
    @Mock private ApplicationUser stashUser;

    private YaccService yaccService;

    @Before
    public void setup() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel","DEBUG");

        MockitoAnnotations.initMocks(this);

        yaccService = new YaccServiceImpl(stashAuthenticationContext, commitsService, jiraService);

        when(stashAuthenticationContext.getCurrentUser()).thenReturn(stashUser);
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_rejectOnMismatch() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("Incorrect Name");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMITTER_NAME,
                "deadbeef: expected committer name 'John Smith' but found 'Incorrect Name'"));
    }
    
    @Test
    public void testCheckRefChange_requireMatchingAuthorName_allowOnMismatchAndExcludedUser() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(settings.getString("excludeUsers")).thenReturn("userName");    
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");
        when(stashUser.getName()).thenReturn("userName");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("Incorrect Name");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_allowOnMatch() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("John Smith");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_notCaseSensitive() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John SMITH");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("John Smith");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_crudIsIgnored() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn(".,:;<>\"\\'John< >\nSMITH.,:;<>\"\\'");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("John Smith");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class)))
                .thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_rejectOnMismatch() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMITTER_EMAIL,
                "deadbeef: expected committer email 'correct@email.com' but found 'wrong@email.com'"));
    }
    
    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_allowOnMismatchAndExcludedUser() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(settings.getString("excludeUsers")).thenReturn("userName");        
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");
        when(stashUser.getName()).thenReturn("userName");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_allowOnMatch() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("correct@email.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_notCaseSensitive() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("CoRrect@EMAIL.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }
    @Test
    public void testCheckRefChange_requireMatchingAuthorEmailRegex_rejectOnMismatch() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(false);
        when(settings.getString("committerEmailRegex")).thenReturn("correct@email.com");
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("wrong@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).contains(new YaccError(YaccError.Type.COMMITTER_EMAIL_REGEX,
                    String.format("deadbeef: committer email regex '%s' does not match user email '%s'",
                            settings.getString("committerEmailRegex"),
                    commit.getCommitter().getEmailAddress())));
    }
    @Test
    public void testCheckRefChange_requireMatchingAuthorEmailRegex_allowOnMatch() throws Exception {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(false);
        when(settings.getString("committerEmailRegex")).thenReturn(".*\\@email.com");
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }
    @Test
    public void testCheckRefChange_serviceUser_skipped() {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.SERVICE);
        
        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getEmailAddress()).thenReturn("CoRrect@EMAIL.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(stashUser, never()).getDisplayName();
        verify(stashUser, never()).getEmailAddress();
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_rejectIfEnabledButNoJiraLinkExists() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(false);

        Set<YaccCommit> commit = Sets.newHashSet(mockCommit());
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(commit);

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError("deadbeef: Unable to verify JIRA issue because JIRA Application Link does not exist"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_rejectIfNoJiraIssuesAreFound() {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit message has no jira issues. abc-123 is not a valid issue because it is lowercase.");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError("deadbeef: No JIRA Issue found in commit message."));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_ignoreUnknownJiraProjectKeys() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getBoolean("ignoreUnknownIssueProjectKeys", false)).thenReturn(true);

        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("ABC", "123"))).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("UTF", "8"))).thenReturn(false);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id and an invalid issue id of UTF-8");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));


        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(jiraService).doesJiraApplicationLinkExist();
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_rejectIfNoJiraIssuesWithAValidProjectAreFound() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getBoolean("ignoreUnknownIssueProjectKeys", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("UTF", "8"))).thenReturn(false);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit message has no jira issues. UTF-8 is not a valid issue because it has an invalid project key.");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError("deadbeef: No JIRA Issue found in commit message."));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_allowedIfValidJiraIssueIsFound() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(jiraService).doesJiraApplicationLinkExist();
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_jiraIssueIdsAreExtractedFromCommitMessage() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("these issue ids should be extracted: ABC-123, ABC_D-123, ABC2-123");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        yaccService.checkRefChange(null, settings, mockRefChange());
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC_D-123"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC2-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_errorsPassedThroughIfTheyAreReturned() {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class)))
                .thenReturn(Lists.newArrayList(new YaccError("some error")));

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));


        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsExactly(new YaccError("deadbeef: some error"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC", "123"));
    }

    @Test
    public void testCheckRefChange_commitMessageRegex_commitMessageMatchesRegex() throws Exception {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("matches regex");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_commitMessageRegex_rejectIfCommitMessageDoesNotMatchRegex() throws Exception {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("123 does not match regex because it contains numbers");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMIT_REGEX,
                "deadbeef: commit message doesn't match regex: [a-z ]+"));
    }

    @Test
    public void testCheckRefChange_excludeByRegex_commitAllowedIfRegexMatches() {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getString("excludeByRegex")).thenReturn("#skipcheck");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit will be allowed #skipcheck");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();

        verify(settings).getString("excludeByRegex");
    }

    @Test
    public void testCheckRefChange_excludeByRegex_commitNotAllowedIfRegexDoesNotMatch() {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getString("excludeByRegex")).thenReturn("#skipcheck");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit will be rejected");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void testCheckRefChange_excludeBranchRegex_commitNotAllowedIfNoJiraIssuesAndBranchIsNotExcluded() {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(settings.getString("excludeBranchRegex")).thenReturn("skipcheck");
        
        MockRefChange refChange = mockRefChange()
                .setRefId("refs/heads/NoSkipcheck");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit message has no jira issues.");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);
        assertThat(errors).containsOnly(new YaccError("deadbeef: No JIRA Issue found in commit message."));
        verify(settings).getString("excludeBranchRegex");
    }

    @Test
    public void testCheckRefChange_excludeBranchRegex_regexMustMatchFullBranchName() {
        when(settings.getString("commitMessageRegex")).thenReturn("[A-Z0-9\\-]+: .*");
        when(settings.getString("excludeBranchRegex")).thenReturn("skipcheck");

        RefChange refChange = mockRefChange()
                .setRefId("refs/heads/some-branch-skipcheck");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit message has no jira issues.");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMIT_REGEX,
                "deadbeef: commit message doesn't match regex: [A-Z0-9\\-]+: .*"));
        verify(settings).getString("excludeBranchRegex");
    }

    @Test
    public void testCheckRefChange_excludeBranchRegex_commitAllowedIfNoJiraIssuesAndBranchIsExcluded() {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getString("excludeBranchRegex")).thenReturn("skipcheck");

        RefChange refChange = mockRefChange()
                .setRefId("refs/heads/skipcheck");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("no JIRA issues but will be allowed anyway");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);
        assertThat(errors).isEmpty();
        verify(settings).getString("excludeBranchRegex");
    }

    @Test
    public void testCheckRefChange_excludeMergeCommits() {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getBoolean("excludeMergeCommits",false)).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("This is a merge commit");
        when(commit.isMerge()).thenReturn(true);
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();

        verify(settings).getBoolean("excludeMergeCommits", false);
    }

    @Test
    public void testCheckRefChange_tag_checksUser() {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccCommit commit = mockCommit();
        when(commit.getCommitter().getName()).thenReturn("Incorrect Name");
        when(commit.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).containsOnly(new YaccError(YaccError.Type.COMMITTER_NAME,
                "deadbeef: expected committer name 'John Smith' but found 'Incorrect Name'"),
                                        new YaccError(YaccError.Type.COMMITTER_EMAIL,
                "deadbeef: expected committer email 'correct@email.com' but found 'wrong@email.com'"));
    }

    @Test
    public void testCheckRefChange_tag_doesntCheckRegex() throws Exception {
        when(settings.getString("commitMessageRegex")).thenReturn("REGEX");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("a message");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_tag_doesntCheckJira() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getBoolean("ignoreUnknownIssueProjectKeys", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist(new IssueKey("UTF", "8"))).thenReturn(false);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("this commit message has no jira issues. UTF-8 is not a valid issue because it has an invalid project key.");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).isEmpty();

        verifyNoMoreInteractions(jiraService);
    }

    @Test
    public void testCheckRefChange_excludeServiceUserCommitsWithInvalidCommitMessage() {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(settings.getBoolean("excludeServiceUserCommits", false)).thenReturn(true);

        when(stashUser.getType()).thenReturn(UserType.SERVICE);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("123 does not match regex because it contains numbers");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(settings).getBoolean("excludeServiceUserCommits", false);
    }

    @Test
    public void testCheckRefChange_excludeUserCommitsWithInvalidCommitMessage() {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(settings.getString("excludeUsers")).thenReturn("excludeUser, nonExcludeUser");

        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getName()).thenReturn("excludeUser");

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("123 does not match regex because it contains numbers");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));

        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(settings).getString("excludeUsers");
    }

    @Test
    public void testCheckRefChange_branchNameRegex_branchRejectedIfDoesNotMatchRegex() {
        when(settings.getString("branchNameRegex")).thenReturn("foo");

        RefChange refChange = mockRefAdd();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);

        assertThat(errors)
                .containsOnly(new YaccError(YaccError.Type.BRANCH_NAME,
                        "Invalid branch name. 'master' does not match regex 'foo'"));
    }

    @Test
    public void testCheckRefChange_branchNameRegex_branchAllowedIfItAlreadyExists() {
        when(settings.getString("branchNameRegex")).thenReturn("foo");

        RefChange refChange = mockRefChange();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);

        assertThat(errors).isEmpty();
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

    private MockRefChange mockRefChange() {
        MockRefChange refChange = new MockRefChange();
        refChange.setFromHash("5773fc438a763e64df8a9c5c32f3b1e83010ada7");
        refChange.setToHash("35d938b060bb361503e021f228e43351f1a71551");
        refChange.setRefId("refs/heads/master");
        refChange.setType(RefChangeType.UPDATE);
        return refChange;
    }

    private MockRefChange mockTagChange() {
        MockRefChange refChange = new MockRefChange();
        refChange.setFromHash("0000000000000000000000000000000000000000");
        refChange.setToHash("35d938b060bb361503e021f228e43351f1a71551");
        refChange.setRefId("refs/tags/tag");
        refChange.setType(RefChangeType.ADD);
        return refChange;
    }
}
