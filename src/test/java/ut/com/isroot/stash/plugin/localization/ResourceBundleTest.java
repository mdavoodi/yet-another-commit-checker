package ut.com.isroot.stash.plugin.localization;

import com.atlassian.stash.setting.Settings;
import com.isroot.stash.plugin.localization.ResourceBundleServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.mockito.Mockito.when;

/**
 * @author Ilya Silin
 * @since 2014-10-07
 */
public class ResourceBundleTest {

    @Mock private Settings settings;

    private ResourceBundleServiceImpl bundle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        bundle = new ResourceBundleServiceImpl();
    }

    @Test
    public void testEn() {
        assertMessagesLoaded("en");
    }

    @Test
    public void testDefaultLocale() {
        assertMessagesLoaded("");
    }

    @Test
    public void testRu() {
        assertMessagesLoaded("ru");
    }

    @Test
    public void testSubstitution() {
        when(settings.getString("messagesLang")).thenReturn("en");
        bundle.refresh(settings);

        String m1 = bundle.getMessage("commitMessageRegex", "[a-Z]");
        Assert.assertEquals("Commit message doesn't match regex: [a-Z]", m1);

        String m2 = bundle.getMessage("incorrectName", "Cool", "Hot");
        Assert.assertEquals("Expected committer name 'Cool' but found 'Hot'", m2);
    }

    private void assertMessagesLoaded(String locale) {
        when(settings.getString("messagesLang")).thenReturn(locale);
        bundle.refresh(settings);

        for (String key : Arrays.asList("errorBearsMessage", "jira.noIssueInCommit")) {
            String value = bundle.getMessage(key);
            Assert.assertNotNull(value);
        }
    }
}
