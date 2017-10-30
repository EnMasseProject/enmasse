package io.enmasse.systemtest.executor.client;

import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.executor.Executor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * Class represent abstract client which keeps common features of client
 */
public abstract class AbstractClient {
    private ClientType clientType;
    private ArrayList<String> arguments;
    private JsonArray messages;

    /**
     * Constructor of abstract class
     * @param clientType type of client
     */
    public AbstractClient(ClientType clientType){
        this.clientType = clientType;
        this.arguments = new ArrayList<>();
        this.arguments.add(ClientType.getCommand(clientType));
        this.messages = new JsonArray();
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
    public void setArguments(Map<Argument, String> args){
        for(Map.Entry<Argument, String> arg : args.entrySet()){
            arguments.add(arg.getKey().command());
            arguments.add(arg.getValue());
        }
    }

    /**
     * Run clients
     * @return true if command end with exit code 0
     */
    public boolean run() {
        try {
            Executor executor = new Executor();
            boolean ret = executor.execute(arguments);
            if (ret) {
                Logging.log.info(executor.getStdOut());
                parseToJson(executor.getStdOut());
            } else {
                Logging.log.info(executor.getStdErr());
            }
            return ret;
        } catch (Exception ex) {
            Logging.log.info(ex.toString());
            return false;
        }
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
}
