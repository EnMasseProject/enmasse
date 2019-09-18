/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.broker.cli;


import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.SyncRequestClient;
import io.vertx.core.Vertx;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.impl.DefaultCLI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class Cli implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(Cli.class.getName());
    private SyncRequestClient brokerClient;
    private Artemis artemis;
    private String command;
    private String queue;


    private boolean parserArgs(String[] args) {

        CLI parser = new DefaultCLI().setName("broker-cli.jar");
        parser.addOption(new Option()
            .setLongName("command")
            .setShortName("c")
            .setRequired(true)
            .addChoice("purgeQueue")
            .addChoice("getMessageCount")
            .setDescription("Command to run on broker"));
        parser.addOption(new Option()
            .setLongName("queue")
            .setShortName("q")
            .setRequired(true)
            .setDescription("Queue to run command against"));

        StringBuilder builder = new StringBuilder();
        parser.usage(builder);

        try {
            CommandLine cl = parser.parse(Arrays.asList(args));
            this.command = cl.getOptionValue("c");
            this.queue = cl.getOptionValue("q");
            return true;
        } catch (Exception e) {
            log.info("\n" + builder.toString());
        }
        return false;
    }

    private void start() throws Exception {

        Vertx vertx = Vertx.vertx();
        ClientFactory clientFactory = new ClientFactory(vertx);
        brokerClient = clientFactory.connectBrokerManagementClient("localhost", 5673);
        if (brokerClient == null) throw new IllegalStateException("Failed to connect to broker localhost:5673");
        artemis = new Artemis(brokerClient);
    }

    private void run() throws TimeoutException {

        switch (command) {
            case "getMessageCount":
                log.info("Count: " + artemis.getQueueMessageCount(queue));
                break;
            case "purgeQueue":
                artemis.purgeQueue(queue);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public static void main(String[] args) {
        try (Cli cli = new Cli()) {
            if (cli.parserArgs(args)) {
                cli.start();
                cli.run();
            } else {
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    @Override
    public void close() {
        if (brokerClient != null) {
            try {
                this.brokerClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
