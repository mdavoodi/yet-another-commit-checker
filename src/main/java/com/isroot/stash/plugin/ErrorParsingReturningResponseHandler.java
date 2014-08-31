package com.isroot.stash.plugin;

import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Bradley Baetz
 */
public class ErrorParsingReturningResponseHandler implements ReturningResponseHandler<Response, String>
{

    @Override
    public String handle(Response response) throws ResponseException
    {
        // Need to retrieve the body here, otherwise its not read
        // at all due to the exception being thrown

        // Also, use the stream method because .getResponseBodyAsString()
        // logs an error to the logs each time
        // The warning is because the resposne body might be too large
        // to buffer in memory, but in our case that's OK (small responses,
        // made to a trusted server)
        String body;
        try
        {
            body = IOUtils.toString(response.getResponseBodyAsStream());
        }
        catch (IOException ex)
        {
            throw new ResponseException(ex);
        }

        if (response.isSuccessful())
        {
            return body;
        }

        if (response.getStatusCode() == 400)
        {
            JsonArray errorMessages = null;
            try
            {
                JsonObject jsonResponse = new JsonParser().parse(body).getAsJsonObject();
                errorMessages = jsonResponse.getAsJsonArray("errorMessages");
            }
            catch (IllegalStateException ex)
            {
                // JSON parse error - fall back to having no errors
            }

            if (errorMessages != null && !Iterables.isEmpty(errorMessages))
            {
                List<String> errors = Lists.newArrayList();

                // Record the actual error
                for (JsonElement err : errorMessages)
                {
                    errors.add(err.getAsString());
                }
                throw new JiraExecutionException("Error response received", errors);
            }
        }

        throw new ResponseStatusException("Unexpected response received. Status code: " + response.getStatusCode(), response);
    }
}
