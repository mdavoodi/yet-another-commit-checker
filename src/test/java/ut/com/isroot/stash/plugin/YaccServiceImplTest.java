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
import com.isroot.stash.plugin.ChangesetsService;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.YaccChangeset;
import com.isroot.stash.plugin.YaccService;
import com.isroot.stash.plugin.YaccServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
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
public class YaccServiceImplTest
{
    @Mock private StashAuthenticationContext stashAuthenticationContext;
    @Mock private ChangesetsService changesetsService;
    @Mock private JiraService jiraService;
    @Mock private ResponseException responseException;
    @Mock private CredentialsRequiredException credRequired;
    @Mock private Settings settings;
    @Mock private StashUser stashUser;

    private YaccService yaccService;

    @Before
    public void setup()
    {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel","DEBUG");

        MockitoAnnotations.initMocks(this);

        yaccService = new YaccServiceImpl(stashAuthenticationContext, changesetsService, jiraService);

        when(stashAuthenticationContext.getCurrentUser()).thenReturn(stashUser);
	}

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_rejectOnMismatch() throws Exception
    {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getName()).thenReturn("Incorrect Name");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
       	assertThat(errors).contains("refs/heads/master: deadbeef: expected committer name 'John Smith' but found 'Incorrect Name'.  To correct this error type: git config --global user.name \"John Smith\"");
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_allowOnMatch() throws Exception
    {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getName()).thenReturn("John Smith");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

		List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
		assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorName_notCaseSensitive() throws Exception
    {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John SMITH");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getName()).thenReturn("John Smith");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_rejectOnMismatch() throws Exception
    {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

		List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
		assertThat(errors).contains("refs/heads/master: deadbeef: expected committer email 'correct@email.com' but found 'wrong@email.com'.  To correct this error type: git config --global user.email \"correct@email.com\"");
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_allowOnMatch() throws Exception
    {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getEmailAddress()).thenReturn("correct@email.com");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

		List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
		assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_requireMatchingAuthorEmail_notCaseSensitive() throws Exception
    {
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getEmailAddress()).thenReturn("CoRrect@EMAIL.com");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }
    
    @Test
    public void testCheckRefChange_serviceUser_skipped()
    {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.SERVICE);
        
        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getEmailAddress()).thenReturn("CoRrect@EMAIL.com");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(stashUser, never()).getDisplayName();
        verify(stashUser, never()).getEmailAddress();
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_rejectIfEnabledButNoJiraLinkExists() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(false);

        Set<YaccChangeset> changesets = Sets.newHashSet(mockChangeset());
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(changesets);

		List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
		assertThat(errors).contains("refs/heads/master: deadbeef: Unable to verify JIRA issue because JIRA Application Link does not exist");
	}

    @Test
    public void testCheckRefChange_requireJiraIssue_rejectIfNoJiraIssuesAreFound() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("this commit message has no jira issues. abc-123 is not a valid issue because it is lowercase.");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

		List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
		assertThat(errors).contains("refs/heads/master: deadbeef: No JIRA Issue found in commit message.");
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_ignoreUnknownJiraProjectKeys() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getBoolean("ignoreUnknownIssueProjectKeys", false)).thenReturn(true);

        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(new IssueKey("ABC-123"))).thenReturn(true);
        when(jiraService.doesProjectExist("ABC")).thenReturn(true);
        when(jiraService.doesProjectExist("UTF")).thenReturn(false);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("ABC-123: this commit has valid issue id and an invalid issue id of UTF-8");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));


        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(jiraService).doesJiraApplicationLinkExist();
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_rejectIfNoJiraIssuesWithAValidProjectAreFound() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getBoolean("ignoreUnknownIssueProjectKeys", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist("UTF")).thenReturn(false);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("this commit message has no jira issues. UTF-8 is not a valid issue because it has an invalid project key.");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).contains("refs/heads/master: deadbeef: No JIRA Issue found in commit message.");
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_allowedIfValidJiraIssueIsFound() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class))).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));


		List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
		assertThat(errors).isEmpty();
        verify(jiraService).doesJiraApplicationLinkExist();
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
	}

    @Test
    public void testCheckRefChange_requireJiraIssue_jiraIssueIdsAreExtractedFromCommitMessage() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("these issue ids should be extracted: ABC-123, ABC_D-123, ABC2-123");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        yaccService.checkRefChange(null, settings, mockRefChange());
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC_D-123"));
        verify(jiraService).doesIssueExist(new IssueKey("ABC2-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_errorReturnedIfNoJiraAuth() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class))).thenThrow(credRequired);
        when(credRequired.getAuthorisationURI()).thenReturn(new URI("http://localhost/link"));

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));


        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).contains("refs/heads/master: deadbeef: ABC-123: Unable to validate JIRA issue because there was an authentication failure when communicating with JIRA.");
        assertThat(errors).contains("refs/heads/master: deadbeef: To authenticate, visit http://localhost/link in a web browser.");
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckRefChange_requireJiraIssue_errorReturnedIfJiraAuthenticationFails() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesIssueExist(any(IssueKey.class))).thenThrow(responseException);
        when(responseException.getCause()).thenReturn(credRequired);
        when(credRequired.getAuthorisationURI()).thenReturn(new URI("http://localhost/link"));

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("ABC-123: this commit has valid issue id");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));


        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).contains("refs/heads/master: deadbeef: ABC-123: Unable to validate JIRA issue because there was an authentication failure when communicating with JIRA.");
        assertThat(errors).contains("refs/heads/master: deadbeef: To authenticate, visit http://localhost/link in a web browser.");
        verify(jiraService).doesIssueExist(new IssueKey("ABC-123"));
    }

    @Test
    public void testCheckRefChange_commitMessageRegex_commitMessageMatchesRegex() throws Exception
    {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("matches regex");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_commitMessageRegex_rejectIfCommitMessageDoesNotMatchRegex() throws Exception
    {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("123 does not match regex because it contains numbers");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).contains("refs/heads/master: deadbeef: commit message doesn't match regex: [a-z ]+");
    }

    @Test
    public void testCheckRefChange_excludeByRegex_commitAllowedIfRegexMatches()
    {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getString("excludeByRegex")).thenReturn("#skipcheck");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("this commit will be allowed #skipcheck");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();

        verify(settings).getString("excludeByRegex");
    }

    @Test
    public void testCheckRefChange_excludeByRegex_commitNotAllowedIfRegexDoesNotMatch()
    {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getString("excludeByRegex")).thenReturn("#skipcheck");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("this commit will be rejected");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isNotEmpty();
    }

    @Test
    public void testCheckRefChange_excludeMergeCommits()
    {
        when(settings.getString("commitMessageRegex")).thenReturn("foo");
        when(settings.getBoolean("excludeMergeCommits",false)).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("This is a merge commit");
        when(changeset.getParentCount()).thenReturn(2);
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();

        verify(settings).getBoolean("excludeMergeCommits", false);
    }


    @Test
    public void testCheckRefChange_tag_checksUserName_withCrud()
    {

        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        String name = "Smith, John P.";
        when(stashUser.getDisplayName()).thenReturn(org.apache.commons.lang.StringUtils.stripEnd(name, ".,:<>\"\\\'"));
        when(stashUser.getEmailAddress()).thenReturn("jsmith@example.com");

        YaccChangeset changeset = mockChangeset_withCrud();
        when(changeset.getCommitter().getName()).thenReturn("Smith, John P");
        when(changeset.getCommitter().getEmailAddress()).thenReturn("jsmith@example.com");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void checkCrudRemover() {
        assert(StringUtils.stripEnd("Hello,.", ".,").equals("Hello"));
    }

        @Test
    public void testCheckRefChange_tag_checksUser()
    {
        when(settings.getBoolean("requireMatchingAuthorName", false)).thenReturn(true);
        when(settings.getBoolean("requireMatchingAuthorEmail", false)).thenReturn(true);
        when(stashUser.getType()).thenReturn(UserType.NORMAL);
        when(stashUser.getDisplayName()).thenReturn("John Smith");
        when(stashUser.getEmailAddress()).thenReturn("correct@email.com");

        YaccChangeset changeset = mockChangeset();
        when(changeset.getCommitter().getName()).thenReturn("Incorrect Name");
        when(changeset.getCommitter().getEmailAddress()).thenReturn("wrong@email.com");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).contains("refs/tags/tag: deadbeef: expected committer name 'John Smith' but found 'Incorrect Name'.  To correct this error type: git config --global user.name \"John Smith\"");
        assertThat(errors).contains("refs/tags/tag: deadbeef: expected committer email 'correct@email.com' but found 'wrong@email.com'.  To correct this error type: git config --global user.email \"correct@email.com\"");
    }


    @Test
    public void testCheckRefChange_tag_doesntCheckRegex() throws Exception
    {
        when(settings.getString("commitMessageRegex")).thenReturn("REGEX");
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("a message");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).isEmpty();
    }

    @Test
    public void testCheckRefChange_tag_doesntCheckJira() throws Exception
    {
        when(settings.getBoolean("requireJiraIssue", false)).thenReturn(true);
        when(settings.getBoolean("ignoreUnknownIssueProjectKeys", false)).thenReturn(true);
        when(jiraService.doesJiraApplicationLinkExist()).thenReturn(true);
        when(jiraService.doesProjectExist("UTF")).thenReturn(false);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("this commit message has no jira issues. UTF-8 is not a valid issue because it has an invalid project key.");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockTagChange());
        assertThat(errors).isEmpty();

        verifyNoMoreInteractions(jiraService);
    }



    @Test
    public void testCheckRefChange_excludeServiceUserCommitsWithInvalidCommitMessage()
    {
        when(settings.getString("commitMessageRegex")).thenReturn("[a-z ]+");
        when(settings.getBoolean("excludeServiceUserCommits", false)).thenReturn(true);

        when(stashUser.getType()).thenReturn(UserType.SERVICE);

        YaccChangeset changeset = mockChangeset();
        when(changeset.getMessage()).thenReturn("123 does not match regex because it contains numbers");
        when(changesetsService.getNewChangesets(any(Repository.class), any(RefChange.class))).thenReturn(Sets.newHashSet(changeset));

        List<String> errors = yaccService.checkRefChange(null, settings, mockRefChange());
        assertThat(errors).isEmpty();
        verify(settings).getBoolean("excludeServiceUserCommits", false);
    }


    private YaccChangeset mockChangeset_withCrud()
    {
        YaccChangeset changeset = mock(YaccChangeset.class, RETURNS_DEEP_STUBS);
        when(changeset.getCommitter().getName()).thenReturn("Smith, John C");
        when(changeset.getCommitter().getEmailAddress()).thenReturn("jsmith@example.com");
        when(changeset.getId()).thenReturn("deadbeef");
        when(changeset.getParentCount()).thenReturn(1);
        return changeset;
    }


    private YaccChangeset mockChangeset()
    {
        YaccChangeset changeset = mock(YaccChangeset.class, RETURNS_DEEP_STUBS);
        when(changeset.getCommitter().getName()).thenReturn("John Smith");
        when(changeset.getCommitter().getEmailAddress()).thenReturn("jsmith@example.com");
		when(changeset.getId()).thenReturn("deadbeef");
		when(changeset.getParentCount()).thenReturn(1);
        return changeset;
    }

    private RefChange mockRefChange()
    {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getFromHash()).thenReturn("5773fc438a763e64df8a9c5c32f3b1e83010ada7");
        when(refChange.getToHash()).thenReturn("35d938b060bb361503e021f228e43351f1a71551");
        when(refChange.getRefId()).thenReturn("refs/heads/master");
        when(refChange.getType()).thenReturn(RefChangeType.UPDATE);
        return refChange;
    }

    private RefChange mockTagChange()
    {
        RefChange refChange = mock(RefChange.class);
        when(refChange.getFromHash()).thenReturn("0000000000000000000000000000000000000000");
        when(refChange.getToHash()).thenReturn("35d938b060bb361503e021f228e43351f1a71551");
        when(refChange.getRefId()).thenReturn("refs/tags/tag");
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        return refChange;
    }
}
