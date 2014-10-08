package ut.com.isroot.stash.plugin.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.isroot.stash.plugin.jira.IssueKey;
import com.isroot.stash.plugin.jira.JiraService;
import com.isroot.stash.plugin.jira.JiraServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2014-01-15
 */
public class JiraServiceImplTest
{
    @Mock
    private ApplicationLinkRequest applicationLinkRequest;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApplicationLinkService applicationLinkService;

    private JiraService jiraService;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        jiraService = new JiraServiceImpl(applicationLinkService);
    }

    @Test
    public void testDoesJiraApplicationLinkExist_returnsFalseIfLinkDoesNotExist() throws Exception
    {
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(null);

        assertThat(jiraService.doesJiraApplicationLinkExist()).isFalse();
    }

    @Test
    public void testDoesJiraApplicationLinkExist_returnsTrueIfLinkExists() throws Exception
    {
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(mock(ApplicationLink.class));

        assertThat(jiraService.doesJiraApplicationLinkExist()).isTrue();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_finalJqlQueryContainsBothIssueKeyAndUserQuery() throws Exception
    {
        jiraService = setupJqlTest("{\"issues\": []}");

        jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));

        verify(applicationLinkRequest).setEntity("{\"jql\":\"issueKey\\u003dTEST-123 and (project \\u003d TEST)\"}");
    }

    @Test
    public void testDoesIssueMatchJqlQuery_httpRequestDetails() throws Exception
    {
        jiraService = setupJqlTest("{\"issues\": []}");

        jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));

        verify(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class).createAuthenticatedRequestFactory())
                .createRequest(Request.MethodType.POST, "/rest/api/2/search");
        verify(applicationLinkRequest).setHeader("Content-Type", "application/json");
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsFalseIfNoIssuesMatchJql() throws Exception
    {
        jiraService = setupJqlTest("{\"issues\": []}");

        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isFalse();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsTrueIfIssuesMatchJql() throws Exception
    {
        jiraService = setupJqlTest("{\"issues\": [{}]}");

        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isTrue();
    }

    @Test
    public void testIsJqlIssueValid_returnsTrueIfValid() throws Exception
    {
        assertThat(jiraService.isJqlQueryValid("assignee is not empty")).isTrue();
    }

    @Test
    public void testIsJqlQueryValid_returnsFalseIfNotValid() throws Exception
    {
        jiraService = setupJqlTest(null);

        ResponseStatusException ex = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
        when(ex.getResponse().getStatusCode()).thenReturn(400);
        when(applicationLinkRequest.execute()).thenThrow(ex);

        assertThat(jiraService.isJqlQueryValid("invalid jql query@#%$")).isFalse();
    }

    @Test
    public void testIsJqlQueryValid_unknownExceptionsAreRethrown() throws Exception
    {
        jiraService = setupJqlTest(null);

        ResponseStatusException ex = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
        when(ex.getResponse().getStatusCode()).thenReturn(500);
        when(applicationLinkRequest.execute()).thenThrow(ex);

        try
        {
            jiraService.isJqlQueryValid("jql query");
            Assert.fail();
        }
        catch(ResponseStatusException expected)
        {
            assertThat(expected).isSameAs(ex);
        }
    }

    private JiraService setupJqlTest(String jsonResponse) throws Exception
    {
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)
                .createAuthenticatedRequestFactory().createRequest(Request.MethodType.POST, "/rest/api/2/search"))
                .thenReturn(applicationLinkRequest);

        jiraService = new JiraServiceImpl(applicationLinkService);

        when(applicationLinkRequest.execute()).thenReturn(jsonResponse);

        return jiraService;
    }
}
