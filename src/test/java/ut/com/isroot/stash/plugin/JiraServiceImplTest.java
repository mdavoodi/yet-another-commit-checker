package ut.com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.google.common.collect.ImmutableList;
import com.isroot.stash.plugin.IssueKey;
import com.isroot.stash.plugin.JiraExecutionException;
import com.isroot.stash.plugin.JiraLookupsException;
import com.isroot.stash.plugin.JiraService;
import com.isroot.stash.plugin.JiraServiceImpl;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import static org.fest.assertions.api.Assertions.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Sean Ford
 * @since 2014-01-15
 */
public class JiraServiceImplTest
{
    @Mock
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
        setupApplicationLink();

        assertThat(jiraService.doesJiraApplicationLinkExist()).isTrue();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_finalJqlQueryContainsBothIssueKeyAndUserQuery() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        ApplicationLinkRequest applicationLinkRequest = setupJqlTest(link, "{\"issues\": [{}]}");

        jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));

        verify(applicationLinkRequest).setEntity("{\"jql\":\"issueKey\\u003dTEST-123 and (project \\u003d TEST)\"}");
    }

    @Test
    public void testDoesIssueMatchJqlQuery_httpRequestDetails() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        ApplicationLinkRequest applicationLinkRequest = setupJqlTest(link, "{\"issues\": [{}]}");

        jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));
        
        setApplicationLinks(link);

        verify(link.createAuthenticatedRequestFactory())
                .createRequest(Request.MethodType.POST, "/rest/api/2/search?fields=summary&validateQuery=false");
        verify(applicationLinkRequest).setHeader("Content-Type", "application/json");
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsFalseIfNoIssuesMatchJql() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        IssueKey issueKey = new IssueKey("TEST", "123");
        
        setupJqlTest(link, "{\"issues\": []}");
        setupIssueTest(link, issueKey, true);

        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", issueKey))
                .isFalse();
    }

    @Test
    public void testDoesIssueMatchJqlQuery_returnsTrueIfIssuesMatchJql() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        setupJqlTest(link, "{\"issues\": [{}]}");

        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isTrue();
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_multipleAppLinks_fallthroughAfterNoMatch() throws Exception
    {
        IssueKey issueKey = new IssueKey("TEST", "123");
        
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        setupJqlTest(link1, "{\"issues\": []}");
        setupIssueTest(link1, issueKey, false);
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        setupJqlTest(link2, "{\"issues\": [{}]}");
        
        setApplicationLinks(link1, link2);
        
        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isTrue();
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_multipleAppLinks_fallthroughAfterError() throws Exception
    {
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        setupJqlTest(link1, mock(ResponseException.class));
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        setupJqlTest(link2, "{\"issues\": [{}]}");
        
        setApplicationLinks(link1, link2);
        
        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isTrue();
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_multipleAppLinks_stopAfterSuccess() throws Exception
    {
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        setupJqlTest(link1, "{\"issues\": [{}]}");
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        
        setApplicationLinks(link1, link2);
        
        assertThat(jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123")))
                .isTrue();
        verifyNoMoreInteractions(link2);
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_multipleAppLinks_allErrorsAreCaptured() throws Exception
    {
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        ResponseException ex1 = mock(ResponseException.class);
        setupJqlTest(link1, ex1);
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        ResponseException ex2 = mock(ResponseException.class);
        setupJqlTest(link2, ex2);
        
        setApplicationLinks(link1, link2);
        
        try
        {
            jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));
            Assert.fail("Exception not thrown");
        }
        catch (JiraLookupsException expected)
        {
            assertThat(expected.getErrors()).hasSize(2)
                    .contains(entry(link1, ex1), entry(link2, ex2));
        }
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_multipleAppLinks_showNotFoundWhereSomeErrors() throws Exception
    {
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        ResponseException ex1 = mock(ResponseException.class);
        setupJqlTest(link1, ex1);
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        setupJqlTest(link2, "{\"issues\": []}");
        
        setApplicationLinks(link1, link2);
        
        try
        {
            jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));
            Assert.fail("Exception not thrown");
        }
        catch (JiraLookupsException expected)
        {
            Map<ApplicationLink, Exception> errors = expected.getErrors();
            assertThat(errors).hasSize(2);
            assertThat(errors).contains(entry(link1, ex1));
            assertThat(errors).containsKey(link2);
            assertThat(errors.get(link2)).hasMessage("TEST-123: JIRA issue does not match JQL query: project = TEST");
        }
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_JQLExceptionMissingJIRA() throws Exception
    {
        IssueKey issue = new IssueKey("TEST", "123");
        ApplicationLink link = setupApplicationLink();
        JiraExecutionException ex = mock(JiraExecutionException.class);
        when(ex.getJiraErrors()).thenReturn(ImmutableList.of(issue.getFullyQualifiedIssueKey()));
        setupJqlTest(link, ex);
        
        assertThat(jiraService.doesIssueMatchJqlQuery(("project = TEST"), issue)).isFalse();
    }
    
    @Test
    public void testDoesIssueMatchJqlQuery_JQLException_ErrorsAreCaptured() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        JiraExecutionException ex = mock(JiraExecutionException.class);
        when(ex.getJiraErrors()).thenReturn(ImmutableList.of("JQL error"));
        setupJqlTest(link, ex);
        
        try
        {
            jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));
            Assert.fail("Exception not thrown");
        }
        catch (JiraLookupsException expected)
        {
            assertThat(expected.getErrors()).hasSize(1)
                    .contains(entry(link, ex));
        }
    }
        
    @Test
    public void testDoesIssueMatchJqlQuery_JQLException_MultipleErrorsAreCaptured() throws Exception
    {
        IssueKey issue = new IssueKey("TEST", "123");
        ApplicationLink link = setupApplicationLink();
        JiraExecutionException ex = mock(JiraExecutionException.class);
        when(ex.getJiraErrors()).thenReturn(ImmutableList.of(issue.getFullyQualifiedIssueKey(), "JQL error"));
        setupJqlTest(link, ex);
        
        try
        {
            jiraService.doesIssueMatchJqlQuery("project = TEST", new IssueKey("TEST", "123"));
            Assert.fail("Exception not thrown");
        }
        catch (JiraLookupsException expected)
        {
            assertThat(expected.getErrors()).hasSize(1)
                    .contains(entry(link, ex));
        }
    }
    
    @Test
    public void testCheckJqlQuery_returnsNoErrorsIfValid() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        setupJqlTest(link, "");
        
        assertThat(jiraService.checkJqlQuery("assignee is not empty")).isEmpty();
    }

    @Test
    public void testCheckJqlQuery_returnsFalseIfNotValid() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        Response resp = mockResponse("{\"errorMessages\":[\"Query error.\"]}", 400);
        setupJqlTest(link, resp);
        
        List<String> errors = jiraService.checkJqlQuery("invalid jql query@#%$");
        assertThat(errors).containsOnly("MOCK JIRA: Query error.");
    }

    @Test
    public void testCheckJqlQuery_unknownExceptionsAreReported() throws Exception
    {
        ApplicationLink link = setupApplicationLink();
        
        ResponseStatusException ex = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
        when(ex.getResponse().getStatusCode()).thenReturn(500);
        when(ex.getResponse().getStatusText()).thenReturn("ERROR");
        
        setupJqlTest(link, ex);
        List<String> errors = jiraService.checkJqlQuery("jql query");

        assertThat(errors).containsOnly("MOCK JIRA: ERROR");
    }
    
    @Test
    public void testCheckJqlQuery_multipleAppLinks_fallThrough() throws Exception
    {
        ResponseException ex = mock(ResponseException.class);
        when(ex.getMessage()).thenReturn("INTERNAL ERROR");
        
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        setupJqlTest(link1, ex);
        
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        setupJqlTest(link2, "");
        
        setApplicationLinks(link1, link2);
        
        assertThat(jiraService.checkJqlQuery("assignee is not empty")).isEmpty();
    }
    
    @Test
    public void testCheckJqlQuery_multipleAppLinks_stopAfterSuccess() throws Exception
    {
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        setupJqlTest(link1, "");
        
        ApplicationLink link2 = mockApplicationLink("JIRA2");
        
        setApplicationLinks(link1, link2);
        
        assertThat(jiraService.checkJqlQuery("assignee is not empty")).isEmpty();
        
        verifyNoMoreInteractions(link2);
    }
    
    @Test
    public void testCheckJqlQuery_multipleAppLinks_allErrorsAreCaptured() throws Exception
    {
        ApplicationLink link1 = mockApplicationLink("JIRA1");
        Response resp1 = mockResponse("", 500);
        setupJqlTest(link1, resp1);

        ApplicationLink link2 = mockApplicationLink("JIRA2");
        Response resp2 = mockResponse("", 501);
        setupJqlTest(link2, resp2);
        
        setApplicationLinks(link1, link2);

        assertThat(jiraService.checkJqlQuery("jql query"))
                .containsOnly("JIRA1: STATUS 500", "JIRA2: STATUS 501");
    }
    
    private void setApplicationLinks(@Nonnull ApplicationLink... links)
    {
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(links[0]);
        when(applicationLinkService.getApplicationLinks(JiraApplicationType.class)).thenReturn(ImmutableList.copyOf(links));
    }
    
    @Nonnull
    private ApplicationLink mockApplicationLink(@Nonnull final String name)
    {
        ApplicationLink link = mock(ApplicationLink.class);
        when(link.getName()).thenReturn(name);
        ApplicationLinkRequestFactory fac = mock(ApplicationLinkRequestFactory.class);
        when(link.createAuthenticatedRequestFactory()).thenReturn(fac);
        return link;
    }
    
    @Nonnull
    private ApplicationLink setupApplicationLink()
    {
        ApplicationLink link = mockApplicationLink("MOCK JIRA");
        setApplicationLinks(link);
        
        return link;
    }

    @Nonnull
    private ApplicationLinkRequest setupJqlTest(@Nonnull final ApplicationLink link, @Nonnull final String jsonResponse) throws Exception
    {
        Response response = mockResponse(jsonResponse, 200);
        return setupJqlTest(link, response);
    }
    
    @Nonnull
    private ApplicationLinkRequest setupJqlTest(@Nonnull final ApplicationLink link, @Nonnull final Response response) throws Exception
    {
        ApplicationLinkRequest req = mock(ApplicationLinkRequest.class);
        when(link.createAuthenticatedRequestFactory().createRequest(Request.MethodType.POST, "/rest/api/2/search?fields=summary&validateQuery=false"))
            .thenReturn(req);
        
        ArgumentCaptor<ReturningResponseHandler> respHandler = ArgumentCaptor.forClass(ReturningResponseHandler.class);
        when(req.executeAndReturn(respHandler.capture())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                ReturningResponseHandler<Response, String> handler = (ReturningResponseHandler<Response, String>)invocation.getArguments()[0];

                return handler.handle(response);
            }
        });

        return req;
    }
    
    @Nonnull
    private ApplicationLinkRequest setupJqlTest(@Nonnull final ApplicationLink link, @Nonnull final Exception ex) throws Exception
    {
        ApplicationLinkRequest req = mock(ApplicationLinkRequest.class);
        when(link.createAuthenticatedRequestFactory().createRequest(Request.MethodType.POST, "/rest/api/2/search?fields=summary&validateQuery=false"))
            .thenReturn(req);

        when(req.execute()).thenThrow(ex);
        when(req.executeAndReturn(any(ReturningResponseHandler.class))).thenThrow(ex);

        return req;
    }
    
    @Nonnull
    private ApplicationLinkRequest setupIssueTest(@Nonnull final ApplicationLink link, @Nonnull IssueKey issueKey, boolean exists) throws Exception
    {
        ApplicationLinkRequest req = mock(ApplicationLinkRequest.class);
        when(link.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "/rest/api/2/issue/"+issueKey.getFullyQualifiedIssueKey()+"?fields=summary"))
            .thenReturn(req);
        
        if (exists)
        {
            when(req.execute()).thenReturn("");
        }
        else
        {
            ResponseStatusException ex = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
            when(ex.getResponse().getStatusCode()).thenReturn(404);
        }
        
        return req;
    }
    
    @Nonnull
    private Response mockResponse(@Nonnull final String jsonResponse, final int statusCode) throws ResponseException
    {
        Response response = mock(Response.class);
        when(response.getResponseBodyAsString()).thenReturn(jsonResponse);
        when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));
        when(response.getStatusCode()).thenReturn(statusCode);
        when(response.getStatusText()).thenReturn("STATUS " + statusCode);
        when(response.isSuccessful()).thenReturn(statusCode >= 200 && statusCode < 300);
        
        return response;
    }
}
