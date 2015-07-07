package ut.com.isroot.stash.plugin;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.StashAuthenticationContext;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.user.UserType;
import com.google.common.collect.Sets;
import com.isroot.stash.plugin.CommitsService;
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

import java.net.URI;
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
    @Mock private StashAuthenticationContext stashAuthenticationContext;
    @Mock private CommitsService commitsService;
    @Mock private JiraService jiraService;
    @Mock private ResponseException responseException;
    @Mock private CredentialsRequiredException credRequired;
    @Mock private Settings settings;
    @Mock private StashUser stashUser;

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
    public void testCheckRefChange_requireJiraIssue_rejectIfNoJiraIssuesAreFound() throws Exception {
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
        when(jiraService.doesIssueExist(new IssueKey("ABC-123"))).thenReturn(true);
        when(jiraService.doesProjectExist("ABC")).thenReturn(true);
        when(jiraService.doesProjectExist("UTF")).thenReturn(false);

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
        when(jiraService.doesProjectExist("UTF")).thenReturn(false);

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
        when(jiraService.doesIssueExist(any(IssueKey.class))).thenReturn(true);

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
    public void testCheckRefChange_requireJiraIssue_errorReturnedIfNoJiraAuth() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class))).thenThrow(credRequired);
        when(credRequired.getAuthorisationURI()).thenReturn(new URI("http://localhost/link"));

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));


        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError("deadbeef: ABC-123: Unable to validate JIRA issue because there was an authentication failure when communicating with JIRA."),
                                        new YaccError("deadbeef: To authenticate, visit http://localhost/link in a web browser."));
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_errorReturnedIfJiraAuthenticationFails() throws Exception {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class))).thenThrow(responseException);
        when(responseException.getCause()).thenReturn(credRequired);
        when(credRequired.getAuthorisationURI()).thenReturn(new URI("http://localhost/link"));

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(commitsService.getNewCommits(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(commit));


        List<YaccError> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).containsOnly(new YaccError("deadbeef: ABC-123: Unable to validate JIRA issue because there was an authentication failure when communicating with JIRA."),
                                        new YaccError("deadbeef: To authenticate, visit http://localhost/link in a web browser."));
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
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
    public void testCheckRefChange_excludeMergeCommits() {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getBoolean("excludeMergeCommits",false)).thenReturn(true);

        YaccCommit commit = mockCommit();
        when(commit.getMessage()).thenReturn("This is a merge commit");
        when(commit.getParentCount()).thenReturn(2);
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
        when(jiraService.doesProjectExist("UTF")).thenReturn(false);

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
    public void testCheckRefChange_branchNameCheckApplied() {
        when(settings.getString("branchNameRegex")).thenReturn("foo");

        RefChange refChange = mockRefChange();

        List<YaccError> errors = yaccService.checkRefChange(null, settings, refChange);

        assertThat(errors)
                .containsOnly(new YaccError(YaccError.Type.BRANCH_NAME,
                        "Invalid branch name. 'master' does not match regex 'foo'"));

    }

    private YaccCommit mockCommit() {
        YaccCommit commit = mock(YaccCommit.class, RETURNS_DEEP_STUBS);
        when(commit.getCommitter().getName()).thenReturn("John Smith");
        when(commit.getCommitter().getEmailAddress()).thenReturn("jsmith@example.com");
        when(commit.getId()).thenReturn("deadbeef");
        when(commit.getParentCount()).thenReturn(1);
        return commit;
    }

    private RefChange mockRefChange() {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getFromHash()).thenReturn("5773fc438a763e64df8a9c5c32f3b1e83010ada7");
        when(refChange.getToHash()).thenReturn("35d938b060bb361503e021f228e43351f1a71551");
        when(refChange.getRefId()).thenReturn("refs/heads/master");
        when(refChange.getType()).thenReturn(RefChangeType.UPDATE);
        return refChange;
    }

    private RefChange mockTagChange() {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getFromHash()).thenReturn("0000000000000000000000000000000000000000");
        when(refChange.getToHash()).thenReturn("35d938b060bb361503e021f228e43351f1a71551");
        when(refChange.getRefId()).thenReturn("refs/tags/tag");
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        return refChange;
    }
}
