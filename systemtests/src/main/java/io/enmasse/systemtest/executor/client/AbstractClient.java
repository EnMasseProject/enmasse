package io.enmasse.systemtest.executor.client;

import io.enmasse.systemtest.executor.Executor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import netscape.javascript.JSObject;

import java.util.ArrayList;

/**
 * Class represent abstract client which keeps common features of client
 */
public abstract class AbstractClient extends Executor {
    private ClientType clientType;
    private ArrayList<String> arguments;
    private JsonArray messgaes;

    /**
     * Constructor of abstract class
     * @param clientType type of client
     */
    public AbstractClient(ClientType clientType){
        this.clientType = clientType;
        this.arguments = new ArrayList<>();
        this.arguments.add(ClientType.getCommand(clientType));
        this.messgaes = new JsonArray();
    }

    /**
     * Get of messages
     * @return Json array of messages;
     */
    public JsonArray getMessgaes() {
        return messgaes;
    }

    /**
     * Set arguments of client
     * @param args string array of arguments
     */
    public void setArguments(String... args){
        for(String arg : args){
            arguments.add(arg);
        }
    }

    /**
     * Run clients
     * @return true if command end with exit code 0
     */
    public boolean run() {
        try {
            boolean ret = super.execute(arguments);
            if (ret) {
                System.out.println(getStdOut());
                parseToJson(getStdOut());
            } else {
                System.out.println(getStdErr());
            }
            return ret;
        } catch (Exception ex) {
            System.out.println(ex.toString());
            return false;
        }
    }

    /**
     * Method for parse string output to json array of messages
     * @param data string data output
     */
    private void parseToJson(String data){
        for(String line : data.split("\n")){
            messgaes.add(new JsonObject(line));
        }
    }
}
