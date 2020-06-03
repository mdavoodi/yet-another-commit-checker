package it.com.isroot.stash.plugin.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sean Ford
 * @since 2015-08-31
 */
public class YaccGlobalSettingsPage extends YaccSettingsCommon {
    @ElementBy(id = "submit")
    private PageElement submit;

    @ElementBy(id = "overrideJiraUserEnabled")
    private PageElement overrideJiraUserEnabledCheckbox;

    @ElementBy(id = "overrideJiraUser")
    private PageElement overrideJiraUserText;

    @Override
    public String getUrl() {
        return "/plugins/servlet/yaccHook/config";
    }

    public YaccSettingsCommon clickSubmit() {
        submit.click();
        waitABitForPageLoad();
        return this;
    }

    public YaccGlobalSettingsPage clickOverrideJiraUserEnabled() {
        overrideJiraUserEnabledCheckbox.click();
        return this;
    }

    public YaccGlobalSettingsPage verifyOverrideJiraUserEnabled(boolean isSelected) {
        assertThat(overrideJiraUserEnabledCheckbox.isSelected()).isEqualTo(isSelected);
        return this;
    }

    public YaccGlobalSettingsPage setOverrideJiraUserText(String regex) {
        overrideJiraUserText.clear();
        overrideJiraUserText.type(regex);
        return this;
    }

    public YaccGlobalSettingsPage verifyOverrideJiraUserText(String regex) {
        assertThat(overrideJiraUserText.getValue()).isEqualTo(regex);
        return this;
    }

}
