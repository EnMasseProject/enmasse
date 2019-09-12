/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.cmdclients;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.executor.Executor;
import io.enmasse.systemtest.logs.CustomLogger;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

public abstract class CmdClient {
    protected static final Object lock = new Object();
    protected static final Environment env = Environment.getInstance();
    private static Logger log = CustomLogger.getLogger();

    protected static ExecutionResultData execute(int timeout, boolean logToOutput, String... cmd) {
        return execute(Arrays.asList(cmd), timeout, logToOutput, true);
    }

    public static ExecutionResultData execute(List<String> command, int timeout, boolean logToOutput) {
        return execute(command, timeout, logToOutput, true);
    }

    public static ExecutionResultData execute(List<String> command, int timeout, boolean logToOutput, boolean appendLineSeparator) {
        try {
            Executor executor = new Executor(appendLineSeparator);
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
