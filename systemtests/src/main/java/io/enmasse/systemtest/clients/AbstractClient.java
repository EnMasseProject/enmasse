/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.executor.Executor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class represent abstract client which keeps common features of client
 */
public abstract class AbstractClient {
    private static Logger log = CustomLogger.getLogger();
    private final Object lock = new Object();
    private final int DEFAULT_ASYNC_TIMEOUT = 120000;
    private final int DEFAULT_SYNC_TIMEOUT = 60000;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSSS");
    protected ArrayList<ClientArgument> allowedArgs = new ArrayList<>();
    private Executor executor;
    private ClientType clientType;
    private JsonArray messages = new JsonArray();
    private ArrayList<String> arguments = new ArrayList<>();
    private Path logPath;

    /**
     * Constructor of abstract client
     *
     * @param clientType type of client
     */
    public AbstractClient(ClientType clientType) {
        this.clientType = clientType;
        this.fillAllowedArgs();
    }

    /**
     * Constructor of abstract client
     *
     * @param clientType type of client
     * @param logPath    path where logs will be stored
     */
    public AbstractClient(ClientType clientType, Path logPath) {
        this.clientType = clientType;
        this.logPath = Paths.get(logPath.toString(), clientType.toString() + "_" + dateFormat.format(new Date()));
        this.fillAllowedArgs();
    }

    /**
     * Get of messages
     *
     * @return Json array of messages;
     */
    public JsonArray getMessages() {
        return messages;
    }

    /**
     * Return type of client
     *
     * @return type of client
     */
    public ClientType getClientType() {
        return clientType;
    }

    /**
     * @param clientType
     */
    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getStdOut() {
        return executor.getStdOut();
    }

    public String getStdErr() {
        return executor.getStdErr();
    }

    /**
     * Set arguments of client
     *
     * @param args string array of arguments
     */
    public void setArguments(ClientArgumentMap args) {
        arguments.clear();
        args = transformArguments(args);
        for (ClientArgument arg : args.getArguments()) {
            if (validateArgument(arg)) {
                for (String value : args.getValues(arg)) {
                    arguments.add(arg.command());
                    arguments.add(value);
                }
            } else {
                log.warn(String.format("Argument '%s' is not allowed for '%s'",
                        arg.command(),
                        this.getClass().getSimpleName()));
            }
        }
    }

    /**
     * Validates that client support this arg
     *
     * @param arg argument to validate
     * @return true if argument is supported
     */
    private boolean validateArgument(ClientArgument arg) {
        return this.allowedArgs.contains(arg);
    }

    /**
     * Fill with clients supported args
     */
    protected abstract void fillAllowedArgs();

    /**
     * Method for modify argument, when client has special address type
     * or connection options etc...
     *
     * @param args argument map of arguments
     * @return modified map of arguments
     */
    protected abstract ClientArgumentMap transformArguments(ClientArgumentMap args);

    /**
     * Method modify executable command of client
     *
     * @param executableCommand command
     * @return list of commands
     */
    protected abstract List<String> transformExecutableCommand(String executableCommand);

    /**
     * Run clients
     *
     * @param timeout kill timeout in ms
     * @return true if command end with exit code 0
     */
    private boolean runClient(int timeout, boolean logToOutput) {
        messages.clear();
        try {
            executor = new Executor(logPath);
            int ret = executor.execute(prepareCommand(), timeout);
            synchronized (lock) {
                log.info("Return code - " + ret);
                if (logToOutput) {
                    if (ret == 0) {
                        log.info(executor.getStdOut());
                        parseToJson(executor.getStdOut());
                    } else {
                        log.error(executor.getStdErr());
                    }
                }
            }
            return ret == 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Merge command and arguments
     *
     * @return merged array of command and args
     */
    private ArrayList<String> prepareCommand() {
        ArrayList<String> command = new ArrayList<>(arguments);
        ArrayList<String> executableCommand = new ArrayList<>();
        executableCommand.addAll(transformExecutableCommand(ClientType.getCommand(clientType)));
        executableCommand.addAll(command);
        return executableCommand;
    }

    /**
     * Run client async
     *
     * @return future of exit status of client
     */
    public Future<Boolean> runAsync() {
        return Executors.newSingleThreadExecutor().submit(() -> runClient(DEFAULT_ASYNC_TIMEOUT, true));
    }

    /**
     * Run client in sync mode
     *
     * @return exit status of client
     */
    public boolean run() {
        return runClient(DEFAULT_SYNC_TIMEOUT, true);
    }

    /**
     * Run client async
     *
     * @param logToOutput enable logging of stdOut and stdErr on output
     * @return future of exit status of client
     */
    public Future<Boolean> runAsync(boolean logToOutput) {
        return Executors.newSingleThreadExecutor().submit(() -> runClient(DEFAULT_ASYNC_TIMEOUT, logToOutput));
    }

    /**
     * Run client async
     *
     * @param logToOutput enable logging of stdOut and stdErr on output
     * @param timeoutInMilliseconds timeout to kill process
     * @return future of exit status of client
     */
    public boolean run(int timeoutInMilliseconds, boolean logToOutput) {
        return runClient(timeoutInMilliseconds, logToOutput);
    }

    /**
     * Run client in sync mode
     *
     * @param logToOutput enable logging of stdOut and stdErr on output
     * @return exit status of client
     */
    public boolean run(boolean logToOutput) {
        return runClient(DEFAULT_SYNC_TIMEOUT, logToOutput);
    }

    /**
     * Run client in sync mode with timeout
     *
     * @param timeout kill timeout in ms
     * @return exit status of client
     */
    public boolean run(int timeout) {
        return runClient(timeout, true);
    }

    /**
     * Method for stop client
     */
    public void stop() {
        try {
            executor.stop();
        } catch (Exception ex) {
            log.warn("Client stop raise exception: " + ex.getMessage());
        }
    }

    /**
     * Method for parse string output to json array of messages
     *
     * @param data string data output
     */
    private void parseToJson(String data) {
        if (data != null) {
            for (String line : data.split(System.getProperty("line.separator"))) {
                if (!Objects.equals(line, "") && !line.trim().isEmpty()) {
                    try {
                        messages.add(new JsonObject(line));
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    //=====================================================================================
    //Default argument transformation
    //=====================================================================================

    /**
     * Base broker transformation to user:password@[ip/hostname]:port
     *
     * @param args argument map
     * @return argument map
     */
    protected ClientArgumentMap basicBrokerTransformation(ClientArgumentMap args) {
        String username;
        String password;
        String broker;
        if (args.getValues(ClientArgument.BROKER) != null) {
            broker = args.getValues(ClientArgument.BROKER).get(0);
            args.remove(ClientArgument.BROKER);

            if (args.getValues(ClientArgument.USERNAME) != null) {
                username = args.getValues(ClientArgument.USERNAME).get(0);
                args.remove(ClientArgument.USERNAME);

                if (args.getValues(ClientArgument.PASSWORD) != null) {
                    password = args.getValues(ClientArgument.PASSWORD).get(0);
                    args.remove(ClientArgument.PASSWORD);

                    args.put(ClientArgument.BROKER, String.format("%s:%s@%s", username, password, broker));
                    return args;
                }

                args.put(ClientArgument.BROKER, String.format("%s:@%s", username, broker));
                return args;
            }

            args.put(ClientArgument.BROKER, broker);
            return args;
        }
        args.remove(ClientArgument.USERNAME);
        args.remove(ClientArgument.PASSWORD);
        return args;
    }

    /**
     * Broker url transformation to amqp[s]://user:password@broker:port/address
     *
     * @param args argument map
     * @return argument map
     */
    protected ClientArgumentMap brokerUrlTranformation(ClientArgumentMap args) {
        args = basicBrokerTransformation(args);
        if (args.getValues(ClientArgument.BROKER) != null) {
            String protocol = args.getValues(ClientArgument.CONN_SSL) != null ? "amqps://" : "amqp://";
            args.put(ClientArgument.BROKER_URL,
                    String.format("%s%s/%s", protocol, args.getValues(ClientArgument.BROKER).get(0),
                            args.getValues(ClientArgument.ADDRESS).get(0)));
        }
        args.remove(ClientArgument.CONN_SSL);
        args.remove(ClientArgument.USERNAME);
        args.remove(ClientArgument.PASSWORD);
        args.remove(ClientArgument.BROKER);
        args.remove(ClientArgument.ADDRESS);

        return args;
    }

    /**
     * Broker java transformation to --broker broker:port
     * --conn-username user --conn-password password --address address
     *
     * @param args argument map
     * @return argument map
     */
    protected ClientArgumentMap javaBrokerTransformation(ClientArgumentMap args) {
        if (args.getValues(ClientArgument.CONN_SSL) != null) {
            if (clientType == ClientType.CLI_JAVA_PROTON_JMS_SENDER
                    || clientType == ClientType.CLI_JAVA_PROTON_JMS_RECEIVER)
                args.put(ClientArgument.BROKER, "amqps://" + args.getValues(ClientArgument.BROKER).get(0));
            if (clientType == ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER
                    || clientType == ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER)
                args.put(ClientArgument.BROKER, "ssl://" + args.getValues(ClientArgument.BROKER).get(0));
            args.put(ClientArgument.CONN_SSL_TRUST_ALL, "true");
            args.put(ClientArgument.CONN_SSL_VERIFY_HOST, (clientType == ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER || clientType == ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER) ? "true" : "false");
            args.put(ClientArgument.CONN_AUTH_MECHANISM, "PLAIN");
        }
        args.remove(ClientArgument.CONN_SSL);

        return args;
    }
}
