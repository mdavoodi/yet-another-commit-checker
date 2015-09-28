package com.isroot.stash.test;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Ford
 * @since 2015-09-27
 */
public class LogTestStartRule extends TestWatcher {
    private static final Logger log = LoggerFactory.getLogger(LogTestStartRule.class);

    @Override
    protected void starting(Description description) {
        log.info("starting test: {}", description.getMethodName());
    }
}
