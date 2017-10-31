package io.enmasse.systemtest.executor;

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
     * @return returns true if command exit code is 0
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public boolean execute(ArrayList<String> commands) throws IOException, InterruptedException, ExecutionException {
        return execute(commands, 0);
    }

    /**
     * Method executes external command
     * @param commands arguments for command
     * @param timeout timeout in ms for kill
     * @return returns true if command exit code is 0
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public boolean execute(ArrayList<String> commands, int timeout) throws IOException, InterruptedException, ExecutionException{
        process = Runtime.getRuntime().exec((commands.toArray(new String[0])));

        Future<String> output = readStdOutput();
        Future<String> error = readStdError();

        int retCode = 1;
        if(timeout > 0){
            if (process.waitFor(timeout, TimeUnit.MILLISECONDS)){
                retCode = process.exitValue();
            }
        }else{
            retCode = process.waitFor();
        }

        stdOut = output.get();
        stdErr = error.get();

        return retCode == 0;
    }

    /**
     * Get standard output of execution
     * @return future string output
     */
    private Future<String> readStdOutput() {
        StreamGobbler errorGobbler = new
                StreamGobbler(process.getInputStream());
        return errorGobbler.read();
    }

    /**
     * Get standard error output of execution
     * @return future string error output
     */
    private Future<String> readStdError(){
        StreamGobbler errorGobbler = new
                StreamGobbler(process.getErrorStream());
        return errorGobbler.read();
    }

    /**
     * Class represent async reader
     */
    class StreamGobbler {
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private InputStream is;

        /**
         * Constructor of StreamGobbler
         * @param is input stream for reading
         */
        StreamGobbler(InputStream is) {
            this.is = is;
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
                StringBuilder builder = new StringBuilder();
                while ( (line = br.readLine()) != null)
                    builder.append(line).append('\n');
                isr.close();
                br.close();
                return builder.toString();
            });

            return future;
        }
    }
}
