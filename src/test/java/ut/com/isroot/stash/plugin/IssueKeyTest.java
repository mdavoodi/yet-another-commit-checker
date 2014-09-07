package ut.com.isroot.stash.plugin;

import com.isroot.stash.plugin.InvalidIssueKeyException;
import com.isroot.stash.plugin.IssueKey;

import java.util.Set;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Test;

public class IssueKeyTest
{
    @Test
    public void testParseIssueKeys() {
        final Set<IssueKey> issueKeys = IssueKey.parseIssueKeys("Issue: ABC-123, CBA-321, UNDER_SCORE-123;");
        assertThat(issueKeys).containsOnly(new IssueKey("ABC", "123"),
                new IssueKey("CBA", "321"), new IssueKey("UNDER_SCORE", "123"));
    }

    @Test
    public void testParseValidIssueKey() throws InvalidIssueKeyException {
        final IssueKey parsed = new IssueKey("ABC-123");
        assertThat(parsed.getProjectKey()).isEqualTo("ABC");
        assertThat(parsed.getIssueId()).isEqualTo("123");
    }

    @Test(expected = InvalidIssueKeyException.class)
    public void testParseInvalidIssueKey() throws InvalidIssueKeyException {
        new IssueKey("invalidkey");
    }

    @Test
    public void testGetFullyQualifiedIssueKey() throws InvalidIssueKeyException {
        assertThat(new IssueKey("ABC", "123").getFullyQualifiedIssueKey()).isEqualTo("ABC-123");
        assertThat(new IssueKey("ABC-123").getFullyQualifiedIssueKey()).isEqualTo("ABC-123");
    }

    @Test
    public void testEquality() throws InvalidIssueKeyException {
        assertThat(new IssueKey("ABC-123")).isEqualTo(new IssueKey("ABC-123"));
        assertThat(new IssueKey("ABC-123").hashCode()).isEqualTo(new IssueKey("ABC-123").hashCode());

        /* Differs by issueId */
        assertThat(new IssueKey("ABC-123")).isNotEqualTo(new IssueKey("ABC-321"));
        assertThat(new IssueKey("ABC-123").hashCode()).isNotEqualTo(new IssueKey("ABC-321").hashCode());

        /* Differs by projectKey */
        assertThat(new IssueKey("ABC-123")).isNotEqualTo(new IssueKey("CBA-321"));
        assertThat(new IssueKey("ABC-123").hashCode()).isNotEqualTo(new IssueKey("CBA-321").hashCode());
    }
}