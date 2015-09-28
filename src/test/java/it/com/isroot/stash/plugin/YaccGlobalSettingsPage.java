package it.com.isroot.stash.plugin;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Ford
 * @since 2015-08-31
 */
public class YaccGlobalSettingsPage extends YaccSettingsCommon {
    private final Logger log = LoggerFactory.getLogger(YaccGlobalSettingsPage.class);

    @ElementBy(id = "submit")
    private PageElement submit;

    @Override
    public String getUrl() {
        return "/plugins/servlet/yaccHook/config";
    }

    public YaccSettingsCommon clickSubmit() {
        log.info("clicking submit");

        submit.click();
        waitABitForPageLoad();
        return this;
    }

}
