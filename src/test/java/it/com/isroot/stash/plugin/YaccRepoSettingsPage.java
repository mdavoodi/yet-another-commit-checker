package it.com.isroot.stash.plugin;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Ford
 * @since 2015-09-13
 */
public class YaccRepoSettingsPage extends YaccSettingsCommon {
    private static final Logger log = LoggerFactory.getLogger(YaccRepoSettingsPage.class);

    @ElementBy(cssSelector = "tr[data-key=\"com.isroot.stash.plugin.yacc:yaccHook\"] .edit-button")
    private PageElement editYacc;

    @ElementBy(cssSelector = "tr[data-key=\"com.isroot.stash.plugin.yacc:yaccHook\"] .mode-disabled")
    private PageElement disableYacc;

    @ElementBy(className = "button-panel-submit-button")
    private PageElement submitButton;

    @Override
    public String getUrl() {
        return "/projects/PROJECT_1/repos/rep_1/settings/hooks";
    }

    public YaccRepoSettingsPage clickEditYacc() {
        log.info("click edit yacc");
        editYacc.click();
        return this;
    }

    public YaccRepoSettingsPage clickSubmit() {
        log.info("click submit");
        submitButton.click();
        waitABitForPageLoad();
        return this;
    }

    public YaccRepoSettingsPage clickDisable() {
        log.info("click disable");
        Poller.waitUntilTrue(disableYacc.timed().isVisible());
        disableYacc.click();
        return this;
    }
}
