/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.IsolatedAddressSpace;
import io.enmasse.systemtest.MarathonTests;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.amqp.AmqpClient;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Category({IsolatedAddressSpace.class, MarathonTests.class})
public class MarathonTestBase extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    ArrayList<AmqpClient> clients = new ArrayList<>();

    @Override
    protected String getDefaultPlan(AddressType addressType) {
        switch (addressType) {
            case QUEUE:
                return "brokered-queue";
            case TOPIC:
                return "brokered-topic";
        }
        return null;
    }

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    protected void runTestInLoop(int durationMinutes, ITestMethod test) {
        log.info(String.format("Starting test running for %d minutes at %s",
                durationMinutes, new Date().toString()));
        int fails = 0;
        int limit = 10;
        for (long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(durationMinutes); stop > System.nanoTime(); ) {
            try {
                test.run();
                fails = 0;
            } catch (Exception ex) {
                collector.addError(ex);
                if (++fails >= limit) {
                    throw new IllegalStateException(String.format("Test failed: %d times in a row", fails));
                }
            } finally {
                closeClients();
            }
        }
    }

    private void closeClients() {
        for (AmqpClient client : clients) {
            try {
                client.close();
                log.info("Client is closed.");
            } catch (Exception ex) {
                collector.addError(ex);
            }
        }
        clients.clear();
    }
}
