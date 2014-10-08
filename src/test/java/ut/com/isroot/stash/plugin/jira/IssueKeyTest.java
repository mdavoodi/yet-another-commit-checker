package ut.com.isroot.stash.plugin.jira;

import com.google.common.collect.Lists;
import com.isroot.stash.plugin.jira.InvalidIssueKeyException;
import com.isroot.stash.plugin.jira.IssueKey;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class IssueKeyTest
{
    @Test
    public void testParseIssueKeys() {
        final List<IssueKey> issueKeys = IssueKey.parseIssueKeys("Issue: ABC-123, CBA-321, UNDER_SCORE-123;");
        assertEquals(issueKeys, Lists.newArrayList(new IssueKey("ABC", "123"),
                new IssueKey("CBA", "321"), new IssueKey("UNDER_SCORE", "123")));
    }

    @Test
    public void testParseValidIssueKey() throws InvalidIssueKeyException {
        final IssueKey parsed = new IssueKey("ABC-123");
        assertEquals("ABC", parsed.getProjectKey());
        assertEquals("123", parsed.getIssueId());
    }

    @Test(expected = InvalidIssueKeyException.class)
    public void testParseInvalidIssueKey() throws InvalidIssueKeyException {
        new IssueKey("invalidkey");
    }

    @Test
    public void testGetFullyQualifiedIssueKey() throws InvalidIssueKeyException {
        assertEquals("ABC-123", new IssueKey("ABC", "123").getFullyQualifiedIssueKey());
        assertEquals("ABC-123", new IssueKey("ABC-123").getFullyQualifiedIssueKey());
    }

    @Test
    public void testEquality() throws InvalidIssueKeyException {
        assertEquals(new IssueKey("ABC-123"), new IssueKey("ABC-123"));
        assertEquals(new IssueKey("ABC-123").hashCode(), new IssueKey("ABC-123").hashCode());

        /* Differs by issueId */
        assertThat(new IssueKey("ABC-123"), is(not(new IssueKey("ABC-321"))));
        assertThat(new IssueKey("ABC-123").hashCode(), is(not(new IssueKey("ABC-321").hashCode())));

        /* Differs by projectKey */
        assertThat(new IssueKey("ABC-123"), is(not(new IssueKey("CBA-123"))));
        assertThat(new IssueKey("ABC-123").hashCode(), is(not(new IssueKey("CBA-123").hashCode())));
    }
}