package com.isroot.stash.plugin;

import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;

import javax.servlet.ServletException;

import java.io.IOException;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.stash.nav.NavBuilder;

import com.atlassian.stash.hook.repository.RepositoryHookService;

/**
 * @author Uldis Ansmits
 * @author Jim Bethancourt
 */
public class YaccConfigServlet extends HttpServlet {

    private static final String SETTINGS_MAP = "com.isroot.stash.plugin.yacc.settings";
    static final String YACC_CONFIG = "com.isroot.stash.plugin.yacc.config";

    private static final long serialVersionUID = 1L;
    private final RepositoryHookService repositoryHookService;
    private static final Logger log = LoggerFactory.getLogger(YaccConfigServlet.class);
    final private SoyTemplateRenderer soyTemplateRenderer;
    private final NavBuilder navBuilder;
    private ConfigValidator configValidator;
    private Map<String, String> fields;
    private Map<String, Iterable<String>> fieldErrors;
    private final PluginSettings pluginSettings;
    private HashMap<String, Object> settingsMap;

    public YaccConfigServlet(SoyTemplateRenderer soyTemplateRenderer,
                             PluginSettingsFactory pluginSettingsFactory,
                             JiraService jiraService,
                             RepositoryHookService repositoryHookService,
                             NavBuilder navBuilder) {
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.navBuilder = navBuilder;
        this.repositoryHookService = repositoryHookService;

        pluginSettings = pluginSettingsFactory.createGlobalSettings();

        configValidator = new ConfigValidator(jiraService);

        fields = new HashMap<String, String>();
        fieldErrors = new HashMap<String, Iterable<String>>();
    }


    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.debug("doGet");
        settingsMap = (HashMap<String, Object>) pluginSettings.get(SETTINGS_MAP);
        if (settingsMap == null) {
            settingsMap = new HashMap<String, Object>();
        }

        validateSettings();
        doGetContinue(req, resp);
    }

    private void validateSettings() {
        Settings settings = repositoryHookService.createSettingsBuilder()
                .addAll(settingsMap)
                .build();
        configValidator.validate(settings, new SettingsValidationErrorsImpl(fieldErrors), null);
    }

    protected void doGetContinue(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        log.debug("doGetContinue");
        fields.clear();

        for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            log.debug("got plugin config " + key + "=" + value + " " + value.getClass().getName());
            if (value instanceof String) {
                fields.put(key, (String) value);
            }
        }

        log.debug("Config fields: " + fields);
        log.debug("Field errors: " + fieldErrors);

        resp.setContentType("text/html;charset=UTF-8");
        try {
            soyTemplateRenderer.render(resp.getWriter(), "com.isroot.stash.plugin.yacc:yaccHook-config-serverside", "com.atlassian.stash.repository.hook.ref.config",
                    ImmutableMap
                            .<String, Object>builder()
                            .put("config", fields)
                            .put("errors", fieldErrors)
                            .build()
            );
        } catch (SoyException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new ServletException(e);
        }

    }

    void addStringFieldValue(HashMap<String, Object> settingsMap, HttpServletRequest req, String fieldName) {
        String o;
        o = req.getParameter(fieldName);
        if (o != null && !o.isEmpty()) settingsMap.put(fieldName, o);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        settingsMap.clear();

        // Plugin globalConfig persister supports onlt map of strings
        while (req.getParameterNames().hasMoreElements()) {
            String parameterName = (String) req.getParameterNames().nextElement();

            // Plugin settings persister only supports map of strings
            if (!parameterName.startsWith("errorMessage") && !parameterName.equals("submit")) {
                addStringFieldValue(settingsMap, req, parameterName);
            }
        }

        validateSettings();

        if (fieldErrors.size() > 0) {
            doGetContinue(req, resp);
            return;
        }

        if(log.isDebugEnabled()) {
            for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                log.debug("save plugin config " + key + "=" + value + " " + value.getClass().getName());
            }
        }

        pluginSettings.put(SETTINGS_MAP, settingsMap);
        pluginSettings.put(YACC_CONFIG, buildYaccConfig());

        String redirectUrl;
        redirectUrl = navBuilder.addons().buildRelative();
        log.debug("redirect: " + redirectUrl);
        resp.sendRedirect(redirectUrl);
    }

    Settings buildYaccConfig() {
        HashMap<String, Object> config = new HashMap<String, Object>();
        for (String fieldName : settingsMap.keySet()) {
            addFieldValueToPluginConfigMap(settingsMap, config, fieldName);
        }

        return repositoryHookService.createSettingsBuilder().addAll(config).build();
    }

    void addFieldValueToPluginConfigMap(HashMap<String, Object> settingsMap, HashMap<String, Object> config, String fieldName) {
        String value = (String) settingsMap.get(fieldName);
        if (value != null && (value.equals("on") || value.equals("true"))) { // handle "on" value
            config.put(fieldName, true);
        } else if (value != null && !value.isEmpty()) {
            config.put(fieldName, value);
        }
    }

    private static class SettingsValidationErrorsImpl implements SettingsValidationErrors {

        Map<String, Iterable<String>> fieldErrors;

        public SettingsValidationErrorsImpl(Map<String, Iterable<String>> fieldErrors) {
            this.fieldErrors = fieldErrors;
            this.fieldErrors.clear();
        }

        @Override
        public void addFieldError(String fieldName, String errorMessage) {
            fieldErrors.put(fieldName, new ArrayList<String>(Arrays.asList(errorMessage)));
        }

        @Override
        public void addFormError(String errorMessage) {
            //not implemented
        }
    }
}