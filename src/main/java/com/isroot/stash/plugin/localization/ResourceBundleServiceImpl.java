package com.isroot.stash.plugin.localization;

import com.atlassian.stash.setting.Settings;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;
import java.util.Properties;

import static com.google.common.base.Strings.*;

/**
 * @author Ilya Silin
 * @since 2014-10-07
 */
public class ResourceBundleServiceImpl implements ResourceBundleService {

    private ReloadableResourceBundleMessageSource bundle;
    private Locale messagesLocale = Locale.getDefault();

    public ResourceBundleServiceImpl() {
        // using Spring for resource bundle loading
        // since it allows specifying properties file encoding other than ISO-8859-1
        bundle = new ReloadableResourceBundleMessageSource();
        bundle.setBasename("localization/messagesBundle");
        // if plugin is redeployed and bundles changed - need to refresh messages
        bundle.setCacheSeconds(5);

        // list properties file encoding if it differs from ISO-8859-1
        Properties props = new Properties();
        props.put("localization/messagesBundle_ru", "UTF-8");
        bundle.setFileEncodings(props);
    }

    @Override
    public void refresh(Settings settings) {
        String lang = settings.getString("messagesLang");
        messagesLocale = new Locale(lang);
    }

    @Override
    public String getMessage(String code, Object ... args) {
        return bundle.getMessage(code, args, messagesLocale);
    }
}
