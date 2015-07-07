package ut.com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.JiraServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2014-01-15
 */
@RunWith(MockitoJUnitRunner.class)
public class JiraServiceImplTest {
    @Mock
    private ApplicationLinkService applicationLinkService;
    @Mock
    private ApplicationLink applicationLink;
    @Mock
    private ApplicationLinkRequestFactory applicationLinkRequestFactory;
    @Mock
    private ApplicationLinkRequest applicationLinkRequest;

    private JiraService jiraService;

    @Before
    public void setup() throws Exception {
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(applicationLink);
        when(applicationLink.createAuthenticatedRequestFactory()).thenReturn(applicationLinkRequestFactory);
        when(applicationLinkRequestFactory.createRequest(Request.MethodType.POST, "/rest/api/2/search"))
                .thenReturn(applicationLinkRequest);

        jiraService = new JiraServiceImpl(applicationLinkService);
    }

    @Test
    public void testDoesJiraApplicationLinkExist_returnsFalseIfLinkDoesNotExist() throws Exception {
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(null);

        assertThat(jiraService.doesJiraApplicationLinkExist()).isFalse();
    }

    @Test
    public void testDoesJiraApplicationLinkExist_returnsTrueIfLinkExists() throws Exception {
        assertThat(jiraService.doesJiraApplicationLinkExist()).isTrue();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_finalJqlQueryContainsBothIssueKeyAndUserQuery() throws Exception {
        setupJqlTest("{\"issues\": []}");

        jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));

        verify(applicationLinkRequest).setEntity("{\"jql\":\"issueKey\\u003dTEST-123 and (project \\u003d TEST)\"}");
    }

    @Test
    public void testDoesIssueMatchJqlQuery_httpRequestDetails() throws Exception {
        setupJqlTest("{\"issues\": []}");

        jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));

        verify(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class).createAuthenticatedRequestFactory())
                .createRequest(Request.MethodType.POST, "/rest/api/2/search");
        verify(applicationLinkRequest).setHeader("Content-Type", "application/json");
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsFalseIfNoIssuesMatchJql() throws Exception {
        setupJqlTest("{\"issues\": []}");

        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isFalse();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsTrueIfIssuesMatchJql() throws Exception {
        setupJqlTest("{\"issues\": [{}]}");

        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isTrue();
    }

    @Test
    public void testIsJqlIssueValid_returnsTrueIfValid() throws Exception {
        assertThat(jiraService.isJqlQueryValid("assignee is not empty")).isTrue();
    }

    @Test
    public void testIsJqlQueryValid_returnsFalseIfNotValid() throws Exception {
        ResponseStatusException ex = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
        when(ex.getResponse().getStatusCode()).thenReturn(400);
        when(applicationLinkRequest.execute()).thenThrow(ex);

        assertThat(jiraService.isJqlQueryValid("invalid jql query@#%$")).isFalse();
    }

    @Test
    public void testIsJqlQueryValid_unknownExceptionsAreRethrown() throws Exception {
        ResponseStatusException ex = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
        when(ex.getResponse().getStatusCode()).thenReturn(500);
        when(applicationLinkRequest.execute()).thenThrow(ex);

        try {
            jiraService.isJqlQueryValid("jql query");
            Assertions.failBecauseExceptionWasNotThrown(ResponseStatusException.class);
        } catch(ResponseStatusException expected) {
            assertThat(expected).isSameAs(ex);
        }
    }

    private void setupJqlTest(String jsonResponse) throws Exception {
        when(applicationLinkRequest.execute()).thenReturn(jsonResponse);
    }
}
