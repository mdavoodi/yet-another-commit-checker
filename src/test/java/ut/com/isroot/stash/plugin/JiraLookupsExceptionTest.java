package ut.com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.google.common.collect.ImmutableMap;
import com.isroot.stash.plugin.JiraLookupsException;
import java.net.URI;
import java.util.List;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

public class JiraLookupsExceptionTest
{
    @Before
    public void setup()
    {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel","DEBUG");

        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void test_messageFormat_credentialsRequired() throws Exception
    {
        CredentialsRequiredException credentialsRequiredException = mock(CredentialsRequiredException.class);
        when(credentialsRequiredException.getAuthorisationURI()).thenReturn(new URI("http://localhost:2990/jira"));
        
        JiraLookupsException ex = createJiraLookupsException("JIRA", credentialsRequiredException);
        
        List<String> errors = ex.getPrintableErrors();
        assertThat(errors).containsOnly("JIRA: Could not authenticate. Visit http://localhost:2990/jira to link your Stash account to your JIRA account");
    }
    
    @Test
    public void test_messageFormat_responseStatus() throws Exception
    {
        ResponseStatusException responseStatusException = mock(ResponseStatusException.class, RETURNS_DEEP_STUBS);
        when(responseStatusException.getResponse().getStatusText()).thenReturn("Response Status Error");
        
        JiraLookupsException ex = createJiraLookupsException("JIRA", responseStatusException);
        
        List<String> errors = ex.getPrintableErrors();
        assertThat(errors).containsOnly("JIRA: Response Status Error");
    }
    
    @Test
    public void test_messageFormat_response()
    {
        ResponseException responseException = mock(ResponseException.class);
        when(responseException.getCause()).thenReturn(null);
        when(responseException.getMessage()).thenReturn("Response Error");
        
        JiraLookupsException ex = createJiraLookupsException("JIRA", responseException);
        
        List<String> errors = ex.getPrintableErrors();
        assertThat(errors).containsOnly("JIRA: Response Error");
    }
    
    @Test
    public void test_messageFormat_responseCause()
    {
        ResponseException responseException = mock(ResponseException.class, RETURNS_DEEP_STUBS);
        when(responseException.getCause().getMessage()).thenReturn("Response Cause Error");
        
        JiraLookupsException ex = createJiraLookupsException("JIRA", responseException);
        
        List<String> errors = ex.getPrintableErrors();
        assertThat(errors).containsOnly("JIRA: Response Cause Error");
    }
    
    @Test
    public void test_messageFormat_otherException()
    {
        Exception e = mock(Exception.class);
        when(e.getMessage()).thenReturn("Exception message");
        
        JiraLookupsException ex = createJiraLookupsException("JIRA", e);
        
        List<String> errors = ex.getPrintableErrors();
        assertThat(errors).containsOnly("JIRA: Internal error: Exception message. Check server logs for details.");
    }
    
    private ApplicationLink mockApplicationLink(String name)
    {
        ApplicationLink link = mock(ApplicationLink.class);
        when(link.getName()).thenReturn(name);
        
        return link;
    }
    
    private JiraLookupsException createJiraLookupsException(String link, Exception error)
    {
        return new JiraLookupsException(ImmutableMap.of(mockApplicationLink(link), error));
    }
}
