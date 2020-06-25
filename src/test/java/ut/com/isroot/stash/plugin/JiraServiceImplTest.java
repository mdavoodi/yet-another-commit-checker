package ut.com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.sal.api.net.Request;
import com.google.gson.Gson;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.errors.YaccError;
import com.isroot.stash.plugin.jira.JiraServiceImpl;
import org.junit.Test;
import ut.com.isroot.stash.plugin.mock.MockApplicationLink;
import ut.com.isroot.stash.plugin.mock.MockApplicationLinkService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sean Ford
 * @since 2014-01-15
 */
public class JiraServiceImplTest {
    @Test
    public void testDoesIssueExist_returnsEmptyListIfJiraSearchResultsIsNonZero() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(1))
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false))
                .isEmpty();
    }

    @Test
    public void testDoesIssueExist_returnsErrorIfJiraSearchResultsIsEmpty() {
        // JIRA API returns a 200 with zero results if project key does not exist

        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0))
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false))
                .containsExactly(new YaccError(YaccError.Type.ISSUE_JQL, "TEST-123: JIRA Issue does not exist"));
    }

    @Test
    public void testDoesIssueExist_returnsErrorIfJiraReturns400() {
        // JIRA API returns a 400 error if project key exists but issue number does not

        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400)
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false))
                .containsExactly(new YaccError(YaccError.Type.ISSUE_JQL, "TEST-123: JIRA Issue does not exist"));
    }

    @Test
    public void testDoesIssueExist_multipleLinks_findIssueIfOneLinkButNotTheOther() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(1))
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false))
                .isEmpty();
    }

    @Test
    public void testDoesIssueExist_multipleLinks_credentialRequiredErrorsIgnoredIfIssueIsFound() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestThrowsCredentialException(),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(1))
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false))
                .isEmpty();
    }

    @Test
    public void testDoesIssueExist_multipleLinks_authErrorsReturnIfAllLinksHaveAuthErrors() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestThrowsCredentialException().setName("jira1"),
                MockApplicationLink.requestThrowsCredentialException().setName("jira2")
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false)).containsExactly(
                new YaccError(YaccError.Type.OTHER, "jira1: Could not authenticate. Visit https://server/auth/uri to link your Stash account to your JIRA account"),
                new YaccError(YaccError.Type.OTHER, "jira2: Could not authenticate. Visit https://server/auth/uri to link your Stash account to your JIRA account"));
    }

    @Test
    public void testDoesIssueExist_multipleLinks_detailedErrorsForAllLinksReturnedIfIssueNotFoundAndAtLeastOneLinkHasAnError() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestThrowsCredentialException().setName("jira1"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)).setName("jira2")
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false)).containsExactly(
                new YaccError(YaccError.Type.OTHER, "jira1: Could not authenticate. Visit https://server/auth/uri to link your Stash account to your JIRA account"),
                new YaccError(YaccError.Type.OTHER, "jira2: TEST-123: JIRA Issue does not exist"));

    }

    @Test
    public void testDoesIssueExist_multipleLinks_issueDoesNotExistInAny() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0))
        );

        assertThat(jiraService.doesIssueExist(new IssueKey("TEST", "123"), false))
                .containsExactly(new YaccError(YaccError.Type.ISSUE_JQL, "TEST-123: JIRA Issue does not exist"));
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsEmptyListIfIssueMatchesJqlQuery() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(1))
        );

        assertThat(jiraService.doesIssueMatchJqlQuery("query", new IssueKey("TEST", "123"), false))
                .isEmpty();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_errorReturnedIfIssueDoesNotMatch() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0))
        );

        assertThat(jiraService.doesIssueMatchJqlQuery("query", new IssueKey("TEST", "123"), false))
                .containsExactly(new YaccError(YaccError.Type.ISSUE_JQL, "TEST-123: JIRA Issue does not match JQL Query: query"));
    }

    @Test
    public void testDoesIssueMatchJqlQuery_errorReturnedIfQueryIsInvalid() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400).setName("jira1")
        );

        assertThat(jiraService.doesIssueMatchJqlQuery("query", new IssueKey("TEST", "123"), false))
                .containsExactly(new YaccError(YaccError.Type.OTHER, "jira1: Query is not valid for JIRA instance: issueKey=TEST-123 and (query)"));
    }

    @Test
    public void testDoesIssueMatchJqlQuery_multipleInstances_errorsIgnoredIfIssueFoundOnAtLeastOneInstance() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestThrowsCredentialException().setName("jira1"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400).setName("jira2"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)).setName("jira3"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(1)).setName("jira4")
        );

        assertThat(jiraService.doesIssueMatchJqlQuery("query", new IssueKey("TEST", "123"), false))
                .isEmpty();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_multipleInstances_detailedErrorsReturnedIfIssueNotFoundAndErrorsOccurred() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestThrowsCredentialException().setName("jira1"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400).setName("jira2"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)).setName("jira3"),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)).setName("jira4")
        );

        assertThat(jiraService.doesIssueMatchJqlQuery("query", new IssueKey("TEST", "123"), false)).containsExactly(
                new YaccError(YaccError.Type.OTHER, "jira1: Could not authenticate. Visit https://server/auth/uri to link your Stash account to your JIRA account"),
                new YaccError(YaccError.Type.OTHER, "jira2: Query is not valid for JIRA instance: issueKey=TEST-123 and (query)"),
                new YaccError(YaccError.Type.OTHER, "jira3: TEST-123: JIRA Issue does not match JQL Query: query"),
                new YaccError(YaccError.Type.OTHER, "jira4: TEST-123: JIRA Issue does not match JQL Query: query"));
    }

    @Test
    public void testDoesIssueMatchJqlQuery_multipleInstances_singleErrorReturnedIfIssueNotFound() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0))
        );

        assertThat(jiraService.doesIssueMatchJqlQuery("query", new IssueKey("TEST", "123"), false)).containsExactly(
                new YaccError(YaccError.Type.ISSUE_JQL, "TEST-123: JIRA Issue does not match JQL Query: query"));
    }

    @Test
    public void testDoesProjectExit_returnsTrueIfJiraReturnsNoSearchResults() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)));

        assertThat(jiraService.doesProjectExist(new IssueKey("TEST", "1"), false))
                .isTrue();
    }

    @Test
    public void testDoesProjectExit_returnsTrueIfJiraReturnsSearchResults() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(1)));

        assertThat(jiraService.doesProjectExist(new IssueKey("TEST", "1"), false))
                .isTrue();
    }

    @Test
    public void testDoesProjectExit_returnsFalseIfJiraReturns400() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400));

        assertThat(jiraService.doesProjectExist(new IssueKey("TEST", "1"), false))
                .isFalse();
    }

    @Test
    public void testCheckJqlQuery_returnsEmptyListIfQueryIsValid() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)));

        assertThat(jiraService.checkJqlQuery("query"))
                .isEmpty();
    }

    @Test
    public void testCheckJqlQuery_returnsErrorIfQueryIsNotValid() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400));

        assertThat(jiraService.checkJqlQuery("query"))
                .containsExactly("JQL Query is invalid.");
    }

    @Test
    public void testCheckJqlQuery_multipleInstances_returnsEmptyListIfQueryIsValidOnAtLeastOneInstance() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400),
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", jiraResponse(0)));

        assertThat(jiraService.checkJqlQuery("query"))
                .isEmpty();
    }

    @Test
    public void testCheckJqlQuery_multipleInstances_detailedErrorsIfQueryIsNotValidOnAtLeastOneInstance() {
        JiraServiceImpl jiraService = setupTest(
                MockApplicationLink.requestReturnsResponse(Request.MethodType.POST, "/rest/api/2/search", 400).setName("jira1"),
                MockApplicationLink.requestThrowsCredentialException().setName("jira2"));

        assertThat(jiraService.checkJqlQuery("query"))
                .containsExactly("jira2: Could not authenticate. Visit https://server/auth/uri to link your Stash account to your JIRA account",
                        "jira1: JQL Query is invalid.");
    }


    private String jiraResponse(int searchResults) {
        List<String> results = new ArrayList<>();

        for (int i = 0; i < searchResults; i++) {
            results.add("");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("issues", results);

        return new Gson().toJson(response);
    }

    private JiraServiceImpl setupTest(ApplicationLink... links) {
        ApplicationLinkService linkService = new MockApplicationLinkService(links);
        return new JiraServiceImpl(linkService);
    }
}
