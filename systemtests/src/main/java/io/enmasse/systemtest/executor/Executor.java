package io.enmasse.systemtest.executor;

import io.enmasse.systemtest.Logging;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Class provide execution of external command
 */
public class Executor {
    private Process process;
    private String stdOut;
    private String stdErr;
    private StreamGobbler stdOutReader;
    private StreamGobbler stdErrReader;

    /**
     * Getter for stdOutput
     * @return string stdOut
     */
    public String getStdOut() {
        return stdOut;
    }

    /**
     * Getter for stdErrorOutput
     * @return string stdErr
     */
    public String getStdErr() {
        return stdErr;
    }

    /**
     * Method executes external command
     * @param commands arguments for command
     * @return returns ecode of execution
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public int execute(ArrayList<String> commands) throws IOException, InterruptedException, ExecutionException {
        return execute(commands, 0);
    }

    /**
     * Method executes external command
     * @param commands arguments for command
     * @param timeout timeout in ms for kill
     * @return returns ecode of execution
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public int execute(ArrayList<String> commands, int timeout) throws IOException, InterruptedException, ExecutionException {
        Logging.log.info("Running command - " + String.join(" ", commands.toArray(new String[0])));
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commands);
        builder.directory(new File(System.getProperty("user.dir")));
        process = builder.start();

        Future<String> output = readStdOutput();
        Future<String> error = readStdError();

        int retCode = 1;
        if(timeout > 0){
            if (process.waitFor(timeout, TimeUnit.MILLISECONDS)){
                retCode = process.exitValue();
            }else{
                process.destroyForcibly();
            }
        }else{
            retCode = process.waitFor();
        }

        shutDownReaders();
        try {
            stdOut = output.get(500, TimeUnit.MILLISECONDS);
        }catch (TimeoutException ex){
            stdOut = stdOutReader.getData();
        }

        try {
            stdErr = error.get(500, TimeUnit.MILLISECONDS);
        }catch (TimeoutException ex){
            stdErr = stdErrReader.getData();
        }

        return retCode;
    }

    /**
     * Get standard output of execution
     * @return future string output
     */
    private Future<String> readStdOutput(){
        stdOutReader = new StreamGobbler(process.getInputStream());
        return stdOutReader.read();
    }

    /**
     * Get standard error output of execution
     * @return future string error output
     */
    private Future<String> readStdError(){
        stdErrReader = new StreamGobbler(process.getErrorStream());
        return stdErrReader.read();
    }

    /**
     * Shutdown process output readers
     */
    private void shutDownReaders(){
        stdOutReader.shutDownReader();
        stdErrReader.shutDownReader();
    }

    /**
     * Class represent async reader
     */
    class StreamGobbler {
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private InputStream is;
        private StringBuilder data = new StringBuilder();

        /**
         * Constructor of StreamGobbler
         * @param is input stream for reading
         */
        StreamGobbler(InputStream is) {
            this.is = is;
        }

        /**
         * Shutdown executor service with reader
         */
        public void shutDownReader(){
            executorService.shutdownNow();
        }

        /**
         * Return data from stream sync
         * @return string of data
         */
        public String getData(){
            return data.toString();
        }

        /**
         * read method
         * @return return future string of output
         */
        public Future<String> read() {
            Future<String> future = executorService.submit(() -> {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ( (line = br.readLine()) != null)
                    data.append(line).append(System.getProperty("line.separator"));
                isr.close();
                br.close();
                return data.toString();
            });

            return future;
        }
    }
}
