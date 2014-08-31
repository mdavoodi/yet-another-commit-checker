package com.isroot.stash.plugin;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.google.common.base.Joiner;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public class JiraLookupsException extends Exception
{
    private final Map<ApplicationLink, Exception> errors;

    public JiraLookupsException(@Nonnull Map<ApplicationLink, Exception> errors) {
        checkNotNull(errors);
        checkState(!errors.isEmpty(), "No errors provided");
        
        this.errors = errors;
    }
    
    @Nonnull
    public Map<ApplicationLink, Exception> getErrors() {
        return errors;
    }
    
    @Nonnull
    public List<String> getPrintableErrors() {
        List<String> ret = new ArrayList<String>();
        
        for (Map.Entry<ApplicationLink, Exception> entry : errors.entrySet())
        {
            String errorStr;
            
            Exception ex = entry.getValue();
            
            if (ex instanceof CredentialsRequiredException)
            {
                CredentialsRequiredException credentialsRequiredException = (CredentialsRequiredException)ex;
                errorStr = "Could not authenticate. Visit " + credentialsRequiredException.getAuthorisationURI().toASCIIString() + " to link your Stash account to your JIRA account";
            }
            else if (ex instanceof ResponseStatusException)
            {
                ResponseStatusException responseStatusException = (ResponseStatusException)ex;
                errorStr = responseStatusException.getResponse().getStatusText();
            }
            else if (ex instanceof ResponseException)
            {
                ResponseException responseException = (ResponseException)ex;
                if (responseException.getCause() != null) {
                    errorStr = responseException.getCause().getMessage();
                } else {
                    errorStr = responseException.getMessage();
                }
            }
            else
            {
                errorStr = "Internal error: " + ex.getMessage() + ". Check server logs for details.";
            }
            
            ret.add(entry.getKey().getName() + ": " + errorStr);
        }
        
        return ret;
    }
    
    @Override
    public String getMessage() {
        return "JIRA lookup errors: " + Joiner.on(", ").join(getPrintableErrors());
    }
}