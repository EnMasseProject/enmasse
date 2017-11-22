package io.enmasse.systemtest;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class SystemTestRunListener extends RunListener {


    @Override
    public void testStarted(Description description) throws Exception {
        Logging.log.info(description + "STARTED");
    }

    @Override
    public void testFinished(Description description) throws Exception {
        Logging.log.info(description + "FINISHED");
    }
}
