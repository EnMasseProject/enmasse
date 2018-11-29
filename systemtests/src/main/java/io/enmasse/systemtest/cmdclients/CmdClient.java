/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.cmdclients;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.executor.Executor;
import org.slf4j.Logger;

import java.util.List;

public class CmdClient {
    private static Logger log = CustomLogger.getLogger();
    protected static final Object lock = new Object();
    protected static final Environment env = new Environment();

    protected static ExecutionResultData execute(List<String> command, int timeout, boolean logToOutput) {
        try {
            Executor executor = new Executor();
            int ret = executor.execute(command, timeout);
            synchronized (lock) {
                if (logToOutput) {
                    log.info("Return code: {}", ret);
                    log.info("stdout: \n{}", executor.getStdOut());
                    log.info("stderr: \n{}", executor.getStdErr());
                }
            }
            return new ExecutionResultData(ret, executor.getStdOut(), executor.getStdErr());
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ExecutionResultData(1, null, null);
        }
    }
}
