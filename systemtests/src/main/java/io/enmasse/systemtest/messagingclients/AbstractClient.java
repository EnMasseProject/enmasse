/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messagingclients;

import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.apps.SystemtestsKubernetesApps;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;

/**
 * Class represent abstract client which keeps common features of client
 */
public abstract class AbstractClient {
    private static Logger log = CustomLogger.getLogger();
    private final Object lock = new Object();
    private final int DEFAULT_ASYNC_TIMEOUT = 300_000;
    private final int DEFAULT_SYNC_TIMEOUT = 60_000;
    protected ArrayList<ClientArgument> allowedArgs = new ArrayList<>();
    private Exec executor;
    private ClientType clientType;
    private JsonArray messages = new JsonArray();
    private ArrayList<String> arguments = new ArrayList<>();
    private Path logPath;
    private List<String> executable;
    private String podName;
    private String podNamespace;
    /**
     * Important: this is not any container_id nor nothing related with amqp, this is just an identifier for logging in our tests
     */
    private String id;

    public AbstractClient(ClientType clientType) throws Exception {
        this(clientType, null, SystemtestsKubernetesApps.MESSAGING_PROJECT);
    }

    public AbstractClient(ClientType clientType, String podNamespace) throws Exception {
        this(clientType, null, podNamespace);
    }

    public AbstractClient(ClientType clientType, Path logPath) throws Exception {
        this(clientType, logPath, SystemtestsKubernetesApps.MESSAGING_PROJECT);
    }

    private AbstractClient(ClientType clientType, Path logPath, String podNamespace) throws Exception {
        this.id = clientType.name() + "-" + UUID.randomUUID().toString();
        this.clientType = clientType;
        if (logPath != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSSS");
            this.logPath = Paths.get(logPath.toString(), clientType.toString() + "_" + dateFormat.format(new Date()));
        }
        this.podNamespace = podNamespace;
        this.podName = SystemtestsKubernetesApps.getMessagingAppPodName(this.podNamespace);
        this.fillAllowedArgs();
        this.executable = transformExecutableCommand(ClientType.getCommand(clientType));
    }

    public String getId() {
        return id;
    }

    public void updateIdWithAddressName(String address) {
        this.id = this.clientType.name() + "-" + address + "-" + UUID.randomUUID().toString();
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public void setPodNamespace(String podNamespace) {
        this.podNamespace = podNamespace;
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
        this.executable = transformExecutableCommand(ClientType.getCommand(clientType));
    }

    /**
     * Get all client arguments
     *
     * @return
     */
    public ArrayList<String> getArguments() {
        return arguments;
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
                log.debug("Argument {} is not allowed for '{}'", arg.command(), this.getClass().getSimpleName());
            }
        }
    }

    public List<String> getExecutable() {
        return this.executable;
    }

    public String getStdOut() {
        return executor.getStdOut();
    }

    public String getStdErr() {
        return executor.getStdErr();
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
            executor = new Exec(logPath);
            int ret = executor.exec(prepareCommand(), timeout, null);
            synchronized (lock) {
                log.info("{} {} Return code - {}", this.getClass().getName(), clientType, ret);
                if (logToOutput) {
                    log.info("{} {} stdout : {}", this.getClass().getName(), clientType, executor.getStdOut());
                }
                if (ret == 0) {
                    parseToJson(executor.getStdOut());
                } else {
                    if (!executor.getStdErr().isEmpty() && logToOutput) {
                        log.error("{} {} stderr : {}", this.getClass().getName(), clientType, executor.getStdErr());
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
        executableCommand.addAll(Arrays.asList(KubeCMDClient.getCMD(), "exec", podName, "-n", podNamespace, "--"));
        executableCommand.addAll(executable);
        executableCommand.addAll(command);
        return executableCommand;
    }

    /**
     * Run client async
     *
     * @return future of exit status of client
     */
    public Future<Boolean> runAsync() {
        return runAsync(true);
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                return runClient(DEFAULT_ASYNC_TIMEOUT, logToOutput);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, runnable -> new Thread(runnable).start());
    }

    /**
     * Run client async
     *
     * @param logToOutput           enable logging of stdOut and stdErr on output
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
                    } catch (Exception ignored) {
                        log.debug("{} - Failed to parse client output '{}' as JSON", clientType, line);
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
                    || clientType == ClientType.CLI_JAVA_PROTON_JMS_RECEIVER) {
                args.put(ClientArgument.BROKER, "amqps://" + args.getValues(ClientArgument.BROKER).get(0));
            }
            if (clientType == ClientType.CLI_JAVA_OPENWIRE_JMS_RECEIVER
                    || clientType == ClientType.CLI_JAVA_OPENWIRE_JMS_SENDER) {
                args.put(ClientArgument.BROKER, "ssl://" + args.getValues(ClientArgument.BROKER).get(0));
            }
            args.put(ClientArgument.CONN_SSL_TRUST_ALL, "true");
            args.put(ClientArgument.CONN_SSL_VERIFY_HOST, (clientType == ClientType.CLI_JAVA_ARTEMIS_JMS_RECEIVER || clientType == ClientType.CLI_JAVA_ARTEMIS_JMS_SENDER) ? "true" : "false");
            args.put(ClientArgument.CONN_AUTH_MECHANISM, "PLAIN");
        }
        args.remove(ClientArgument.CONN_SSL);

        return args;
    }
}
