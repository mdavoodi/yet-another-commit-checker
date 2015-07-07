package com.isroot.stash.plugin.checks;

import com.atlassian.stash.scm.git.GitRefPattern;
import com.atlassian.stash.setting.Settings;
import com.isroot.stash.plugin.errors.YaccError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sean Ford
 * @since 2015-04-25
 */
public class BranchNameCheck {
    private final static Logger log = LoggerFactory.getLogger(BranchNameCheck.class);

    private final Settings settings;
    private final String refId;

    public BranchNameCheck(Settings settings, String refId) {
        this.settings = settings;
        this.refId = refId;
    }

    public List<YaccError> check() {
        List<YaccError> errors = new ArrayList<>();
        boolean isBranch = refId.startsWith(GitRefPattern.HEADS.getPath());

        Pattern branchNamePattern = getPattern();

        if (isBranch && branchNamePattern != null) {
            String branchName = refId.replace(GitRefPattern.HEADS.getPath(), "");
            Matcher matcher = branchNamePattern.matcher(branchName);

            log.debug("checking branch name {} with regex {}, matches={}", branchName, getRegex(),
                    matcher.matches());

            if (!matcher.matches()) {
                errors.add(new YaccError(YaccError.Type.BRANCH_NAME,
                        String.format("Invalid branch name. '%s' does not match regex '%s'",
                                branchName, settings.getString("branchNameRegex"))));
            }
        }

        return errors;
    }

    private String getRegex() {
        return settings.getString("branchNameRegex");
    }

    private Pattern getPattern() {
        String regex = getRegex();
        if (regex == null || regex.isEmpty()) {
            return null;
        }

        return Pattern.compile(regex);
    }
}
