package it.com.isroot.stash.plugin.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
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

    @ElementBy(cssSelector = ".aui-dialog2-footer .aui-button-primary")
    private PageElement submitButton;

    @ElementBy(cssSelector = ".hooks-table-pre-receive tr:nth-of-type(4) .inherit-toggle")
    private PageElement inheritToggle;

    @ElementBy(id = "react-select-5--option-0")
    private PageElement inheritedOption;

    @ElementBy(id = "react-select-5--option-1")
    private PageElement enabledOption;

    @ElementBy(id = "react-select-5--option-2")
    private PageElement disabledOption;

    @Override
    public String getUrl() {
        return "/projects/PROJECT_1/repos/rep_1/settings/hooks";
    }

    public YaccRepoSettingsPage clickEnableYacc() {
        // First time hook is enabled, settings modal pops up. Disable it before enabling to
        // force settings modal to pop up. This overloads method to both enable and bring up
        // settings in one go.
        // Probably not great but it works :-)
        clickDisable();

        log.info("enabling yacc");

        inheritToggle.select();
        enabledOption.click();

        return this;
    }

    public YaccRepoSettingsPage clickSubmit() {
        submitButton.click();
        waitABitForPageLoad();
        return this;
    }

    public YaccRepoSettingsPage clickDisable() {
        log.info("disabling yacc");

        inheritToggle.select();
        disabledOption.click();

        return this;
    }
}
