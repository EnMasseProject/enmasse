package io.enmasse.systemtest.executor.client;

import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.executor.Executor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Class represent abstract client which keeps common features of client
 */
public abstract class AbstractClient {
    private final int DEFAULT_ASYNC_TIMEOUT = 120000;
    private final int DEFAULT_SYNC_TIMEOUT = 60000;

    private JsonArray messages = new JsonArray();;
    private ArrayList<String> arguments = new ArrayList<>();
    protected ArrayList<Argument> allowedArgs = new ArrayList<>();

    /**
     * Constructor of abstract client
     * @param clientType type of client
     */
    public AbstractClient(ClientType clientType){
        this.arguments.add(ClientType.getCommand(clientType));
        this.fillAllowedArgs();
    }

    /**
     * Get of messages
     * @return Json array of messages;
     */
    public JsonArray getMessages() {
        return messages;
    }

    /**
     * Set arguments of client
     * @param args string array of arguments
     */
    public void setArguments(ArgumentMap args){
        args = transformArguments(args);
        for(Argument arg : args.getArguments()){
            if(validateArgument(arg)) {
                for(String value : args.getValues(arg)){
                    arguments.add(arg.command());
                    arguments.add(value);
                }
            }else{
                Logging.log.warn(String.format("Argument '%s' is not allowed for '%s'",
                        arg.command(),
                        this.getClass().getSimpleName()));
            }
        }
    }

    /**
     * Validates that client support this arg
     * @param arg argument to validate
     * @return true if argument is supported
     */
    private boolean validateArgument(Argument arg){
        return this.allowedArgs.contains(arg);
    }

    /**
     * Fill with clients supported args
     */
    protected abstract void fillAllowedArgs();

    /**
     * Method for modify argument, when client has special address type
     * or connection options etc...
     * @param args argument map of arguments
     * @return modified map of arguments
     */
    protected abstract ArgumentMap transformArguments(ArgumentMap args);

    /**
     * Run clients
     * @param timeout kill timeout in ms
     * @return true if command end with exit code 0
     */
    private boolean runClient(int timeout) {
        try {
            Executor executor = new Executor();
            boolean ret = executor.execute(arguments, timeout);
            if (ret) {
                Logging.log.info(executor.getStdOut());
                parseToJson(executor.getStdOut());
            } else {
                Logging.log.error(executor.getStdErr());
            }
            return ret;
        } catch (Exception ex) {
            Logging.log.error(ex.toString());
            return false;
        }
    }

    /**
     * Run client async
     * @return future of exit status of client
     */
    public Future<Boolean> runAsync(){
        return Executors.newSingleThreadExecutor().submit(() -> runClient(DEFAULT_ASYNC_TIMEOUT));
    }

    /**
     * Run client in sync mode
     * @return exit status of client
     */
    public boolean run(){
        return runClient(DEFAULT_SYNC_TIMEOUT);
    }

    /**
     * Run client in sync mode with timeout
     * @param timeout kill timeout in ms
     * @return exit status of client
     */
    public boolean run(int timeout){
        return runClient(timeout);
    }

    /**
     * Method for parse string output to json array of messages
     * @param data string data output
     */
    private void parseToJson(String data){
        for(String line : data.split("\n")){
            messages.add(new JsonObject(line));
        }
    }

    //=====================================================================================
    //Default argument transformation
    //=====================================================================================

    /**
     * Base broker transformation to user:password@[ip/hostname]:port
     * @param args argument map
     * @return
     */
    protected ArgumentMap basicBrokerTransformation(ArgumentMap args){
        String username;
        String password;
        String broker;
        if(args.getValues(Argument.BROKER) != null){
            broker = args.getValues(Argument.BROKER).get(0);
            args.remove(Argument.BROKER);

            if(args.getValues(Argument.USERNAME) != null){
                username = args.getValues(Argument.USERNAME).get(0);
                args.remove(Argument.USERNAME);

                if(args.getValues(Argument.PASSWORD) != null){
                    password = args.getValues(Argument.PASSWORD).get(0);
                    args.remove(Argument.PASSWORD);

                    args.put(Argument.BROKER, String.format("%s:%s@%s", username, password, broker));
                    return args;
                }

                args.put(Argument.BROKER, String.format("%s:@%s", username, broker));
                return args;
            }

            args.put(Argument.BROKER, broker);
            return args;
        }
        args.remove(Argument.USERNAME);
        args.remove(Argument.PASSWORD);
        return args;
    }

}
