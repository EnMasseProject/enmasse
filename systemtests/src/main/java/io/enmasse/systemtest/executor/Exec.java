/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.executor;

import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.logs.CustomLogger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Class provide execution of external command
 */
public class Exec {
    private static Logger log = CustomLogger.getLogger();
    private Process process;
    private String stdOut;
    private String stdErr;
    private StreamGobbler stdOutReader;
    private StreamGobbler stdErrReader;
    private Path logPath;
    private boolean appendLineSeparator;
    private static final Pattern PATH_SPLITTER = Pattern.compile(System.getProperty("path.separator"));
    protected static final Object lock = new Object();
    protected static final Environment env = Environment.getInstance();

    public Exec() {
        this.appendLineSeparator = true;
    }

    public Exec(Path logPath) {
        this.appendLineSeparator = true;
        this.logPath = logPath;
    }

    public Exec(boolean appendLineSeparator) {
        this.appendLineSeparator = appendLineSeparator;
    }

    /**
     * Getter for stdOutput
     *
     * @return string stdOut
     */
    public String getStdOut() {
        return stdOut;
    }

    /**
     * Getter for stdErrorOutput
     *
     * @return string stdErr
     */
    public String getStdErr() {
        return stdErr;
    }

    public boolean isRunning() {
        return process.isAlive();
    }

    public int getRetCode() {
        if (isRunning()) {
            return -1;
        } else {
            return process.exitValue();
        }
    }

    /**
     * Method executes external command
     *
     * @param commands arguments for command
     * @return returns ecode of execution
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public int exec(List<String> commands) throws IOException, InterruptedException, ExecutionException {
        return exec(commands, 0, null);
    }

    /**
     * Method executes external command
     *
     * @param commands arguments for command
     * @param timeout  timeout in ms for kill
     * @return returns ecode of execution
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public int exec(List<String> commands, int timeout, Map<String, String> env) throws IOException, InterruptedException, ExecutionException {
        log.info("Running command - " + String.join(" ", commands.toArray(new String[0])));
        ProcessBuilder builder = new ProcessBuilder();
        if (env != null) {
            builder.environment().putAll(env);
        }
        builder.command(commands);
        builder.directory(new File(System.getProperty("user.dir")));
        process = builder.start();

        Future<String> output = readStdOutput();
        Future<String> error = readStdError();

        int retCode = 1;
        if (timeout > 0) {
            if (process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
                retCode = process.exitValue();
            } else {
                process.destroyForcibly();
            }
        } else {
            retCode = process.waitFor();
        }

        try {
            stdOut = output.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            output.cancel(true);
            stdOut = stdOutReader.getData();
        }

        try {
            stdErr = error.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            error.cancel(true);
            stdErr = stdErrReader.getData();
        }
        storeOutputsToFile();
        return retCode;
    }

    /**
     * Method kills process
     */
    public void stop() {
        process.destroyForcibly();
        stdOut = stdOutReader.getData();
        stdErr = stdErrReader.getData();
    }

    /**
     * Get standard output of execution
     *
     * @return future string output
     */
    private Future<String> readStdOutput() {
        stdOutReader = new StreamGobbler(process.getInputStream());
        return stdOutReader.read();
    }

    /**
     * Get standard error output of execution
     *
     * @return future string error output
     */
    private Future<String> readStdError() {
        stdErrReader = new StreamGobbler(process.getErrorStream());
        return stdErrReader.read();
    }

    /**
     * Get stdOut and stdErr and store it into files
     */
    private void storeOutputsToFile() {
        if (logPath != null) {
            try {
                Files.createDirectories(logPath);
                Files.write(Paths.get(logPath.toString(), "stdOutput.log"), stdOut.getBytes());
                Files.write(Paths.get(logPath.toString(), "stdError.log"), stdErr.getBytes());
            } catch (Exception ex) {
                log.warn("Cannot save output of execution", ex);
            }
        }
    }

    public static boolean isExecutableOnPath(String cmd) {
        for (String dir : PATH_SPLITTER.split(System.getenv("PATH"))) {
            if (new File(dir, cmd).canExecute()) {
                return true;
            }
        }
        return false;
    }

    public static ExecutionResultData execute(List<String> command) {
        return execute(command, 60_000, true, true);
    }

    public static ExecutionResultData execute(List<String> command, boolean logToOutput) {
        return execute(command, 60_000, logToOutput, true);
    }

    public static ExecutionResultData execute(int timeout, boolean logToOutput, String... cmd) {
        return execute(Arrays.asList(cmd), timeout, logToOutput, true);
    }

    public static ExecutionResultData execute(List<String> command, int timeout, boolean logToOutput) {
        return execute(command, timeout, logToOutput, true);
    }

    public static ExecutionResultData execute(List<String> command, int timeout, boolean logToOutput, boolean appendLineSeparator) {
        return execute(command, timeout, logToOutput, appendLineSeparator, null);
    }

    public static ExecutionResultData execute(List<String> command, int timeout, boolean logToOutput, boolean appendLineSeparator, Map<String, String> env) {
        try {
            Exec executor = new Exec(appendLineSeparator);
            int ret = executor.exec(command, timeout, env);
            synchronized (lock) {
                if (logToOutput) {
                    log.info("Return code: {}", ret);
                    if (!executor.getStdOut().equals("")) {
                        log.info("stdout: \n{}", executor.getStdOut());
                    }
                    if (!executor.getStdErr().equals("")) {
                        log.info("stderr: \n{}", executor.getStdErr());
                    }
                }
            }
            return new ExecutionResultData(ret, executor.getStdOut(), executor.getStdErr());
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ExecutionResultData(1, null, null);
        }
    }

    /**
     * Class represent async reader
     */
    class StreamGobbler {
        private InputStream is;
        private StringBuilder data = new StringBuilder();

        /**
         * Constructor of StreamGobbler
         *
         * @param is input stream for reading
         */
        StreamGobbler(InputStream is) {
            this.is = is;
        }

        /**
         * Return data from stream sync
         *
         * @return string of data
         */
        public String getData() {
            return data.toString();
        }

        /**
         * read method
         *
         * @return return future string of output
         */
        public Future<String> read() {
            return CompletableFuture.supplyAsync(() -> {
                Scanner scanner = new Scanner(is);
                try {
                    log.debug("Reading stream {}", is);
                    while (scanner.hasNextLine()) {
                        data.append(scanner.nextLine());
                        if (appendLineSeparator) {
                            data.append(System.getProperty("line.separator"));
                        }
                    }
                    scanner.close();
                    return data.toString();
                } catch (Exception e) {
                    throw new CompletionException(e);
                } finally {
                    scanner.close();
                }
            }, runnable -> new Thread(runnable).start());
        }
    }
}
