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
import com.atlassian.stash.server.ApplicationPropertiesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.stash.nav.NavBuilder;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.stash.hook.repository.RepositoryHookService;


public class ConfigServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final RepositoryHookService repositoryHookService;
    private static final Logger log = LoggerFactory.getLogger(ConfigServlet.class);
    final private SoyTemplateRenderer soyTemplateRenderer;
    //    private final ApplicationPropertiesService applicationPropertiesService;
    private final JiraService jiraService;
    private final NavBuilder navBuilder;
    Settings settings;
    ConfigValidator configValidator;
    private Map<String, String> fields;
    private Map<String, Iterable<String>> fieldErrors;
    private final PluginSettings pluginSettings;
    static final String SETTINGS_MAP="com.isroot.stash.plugin.yacc.settings";
    HashMap<String, Object> settingsMap;

    public ConfigServlet(SoyTemplateRenderer soyTemplateRenderer,
            PluginSettingsFactory pluginSettingsFactory,
            ApplicationPropertiesService applicationPropertiesService,
            JiraService jiraService,
            RepositoryHookService repositoryHookService,
            NavBuilder navBuilder
    )
    {
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.jiraService = jiraService;
        this.navBuilder= navBuilder;
        this.repositoryHookService = repositoryHookService;

        pluginSettings = pluginSettingsFactory.createGlobalSettings();

        configValidator=new ConfigValidator(jiraService);


        fields = new HashMap<String, String>();
        fieldErrors=new HashMap<String, Iterable<String>>();
    }


    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        log.debug("doGet");
        settingsMap = (HashMap<String, Object>) pluginSettings.get(SETTINGS_MAP);
        if(settingsMap == null) {
            settingsMap = new HashMap<String, Object>();
        }
        settings=repositoryHookService.createSettingsBuilder()
                .addAll(settingsMap)
                .build();
        //    settings=new MapSettingsBuilder().addAll(settingsMap).build();
        //configValidator.validate(settings, new SettingsValidationErrorsImpl(errors), null);
        configValidator.validate(settings, new SettingsValidationErrorsImpl(fieldErrors), null);
        doGetContinue(req,resp);
    }
    protected void doGetContinue(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        log.debug("doGetContinue");
        fields.clear();

        for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            log.debug("got plugin config "+key+"="+value+" "+value.getClass().getName());
            if(value instanceof String){
                fields.put(key,(String)value);
            }
        }


        log.debug("Config fields: "+fields);
        log.debug("Field errors: "+fieldErrors);

        resp.setContentType("text/html;charset=UTF-8");
        try
        {
            soyTemplateRenderer.render(resp.getWriter(), "com.isroot.stash.plugin.yacc:yaccHook-config-serverside", "com.atlassian.stash.repository.hook.ref.config", 
                    ImmutableMap
                    .<String, Object>builder()
                    .put("config",fields)
                    .put("errors",fieldErrors)
                    .build()
            );
            return;
        } catch (SoyException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new ServletException(e);
        }

    }

    void addStringFieldValue(HashMap<String, Object> settingsMap,HttpServletRequest req,String fieldName) {
        String o;
        o=req.getParameter(fieldName);
        if(o!=null && !o.isEmpty())settingsMap.put(fieldName,o);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException
    {
        settingsMap.clear();

        // Plugin settings persister supports onlt map of strings
        addStringFieldValue(settingsMap,req,"requireMatchingAuthorEmail");
        addStringFieldValue(settingsMap,req,"requireMatchingAuthorName");
        addStringFieldValue(settingsMap,req,"commitMessageRegex");
        addStringFieldValue(settingsMap,req,"requireJiraIssue");
        addStringFieldValue(settingsMap,req,"ignoreUnknownIssueProjectKeys");
        addStringFieldValue(settingsMap,req,"issueJqlMatcher");
        addStringFieldValue(settingsMap,req,"excludeMergeCommits");
        addStringFieldValue(settingsMap,req,"excludeByRegex");
        addStringFieldValue(settingsMap,req,"excludeServiceUserCommits");

        settings=repositoryHookService.createSettingsBuilder()
                .addAll(settingsMap)
                .build();

        configValidator.validate(settings, new SettingsValidationErrorsImpl(fieldErrors), null);

        if (fieldErrors.size()>0) {
            doGetContinue(req,resp);
            return;
        }
        for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            log.debug("save plugin config "+key+"="+value+" "+value.getClass().getName());
        }
        pluginSettings.put(SETTINGS_MAP, settingsMap);
        String redirectUrl;
        redirectUrl=navBuilder.addons().buildRelative();        
        log.debug("redirect: "+redirectUrl);
        resp.sendRedirect(redirectUrl);
    }
    private static class SettingsValidationErrorsImpl implements SettingsValidationErrors {

        Map<String, Iterable<String>> fieldErrors;
        public SettingsValidationErrorsImpl(Map<String, Iterable<String>> fieldErrors) {
            this.fieldErrors=fieldErrors;
            this.fieldErrors.clear();
        }

        @Override
        public void addFieldError(String fieldName, String errorMessage) {
            fieldErrors.put(fieldName, new ArrayList<String>(Arrays.asList(errorMessage)));
        }

        @Override
        public void addFormError(String errorMessage) {
            //not implemented
            return; 
        }
    }
}
