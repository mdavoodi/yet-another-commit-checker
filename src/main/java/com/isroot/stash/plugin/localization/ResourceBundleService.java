package com.isroot.stash.plugin.localization;

import com.atlassian.stash.setting.Settings;

/**
 * @author Ilya Silin
 * @since 2014-10-07
 */
public interface ResourceBundleService {

    void refresh(Settings settings);

    String getMessage(String code, Object ... args);
}
