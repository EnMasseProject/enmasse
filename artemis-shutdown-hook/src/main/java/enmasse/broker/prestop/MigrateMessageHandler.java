/*
 *  Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import javafx.util.Pair;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO: Description
 */
public class MigrateMessageHandler {
    private final AtomicReference<ProtonReceiver> protonReceiver = new AtomicReference<>();
    private final AtomicReference<ProtonSender> protonSender = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> latch = new AtomicReference<>();
    private volatile boolean ready = false;
    private final Endpoint toEndpoint;
    private final String queueName;
    private final String clientId;
    private final String subscriptionName;

    public MigrateMessageHandler(String queueName, Endpoint toEndpoint) {
        Pair<String, String> pair = decomposeQueueNameForDurableSubscription(queueName);
        this.queueName = queueName;
        this.clientId = pair.getKey();
        this.subscriptionName = pair.getValue();
        this.toEndpoint = toEndpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public String getQueueName() {
        return queueName;
    }


    // This code is stolen from Artemis internals so that we can decompose the queue name in the same way. This will not be necessary once we can use global link names.
    private static final char SEPARATOR = '.';
    private static Pair<String, String> decomposeQueueNameForDurableSubscription(final String queueName) {
        StringBuffer[] parts = new StringBuffer[2];
        int currentPart = 0;

        parts[0] = new StringBuffer();
        parts[1] = new StringBuffer();

        int pos = 0;
        while (pos < queueName.length()) {
            char ch = queueName.charAt(pos);
            pos++;

            if (ch == SEPARATOR) {
                currentPart++;
                if (currentPart >= parts.length) {
                    throw new RuntimeException("Invalid message queue name: " + queueName);
                }

                continue;
            }

            if (ch == '\\') {
                if (pos >= queueName.length()) {
                    throw new RuntimeException("Invalid message queue name: " + queueName);
                }
                ch = queueName.charAt(pos);
                pos++;
            }

            parts[currentPart].append(ch);
        }

        if (currentPart != 1) {
         /* JMS 2.0 introduced the ability to create "shared" subscriptions which do not require a clientID.
          * In this case the subscription name will be the same as the queue name, but the above algorithm will put that
          * in the wrong position in the array so we need to move it.
          */
            parts[1] = parts[0];
            parts[0] = new StringBuffer();
        }

        Pair<String, String> pair = new Pair<>(parts[0].toString(), parts[1].toString());

        return pair;
    }


    public static boolean isValidSubscription(String queue) {
        try {
            decomposeQueueNameForDurableSubscription(queue);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public ProtonMessageHandler messageHandler() {
        return (sourceDelivery, message) -> protonSender.get().send(message, protonDelivery -> {
            System.out.println("Forwarding message to subscriber");
            message.setPriority(Short.MAX_VALUE);
            sourceDelivery.disposition(protonDelivery.getRemoteState(), protonDelivery.remotelySettled());
            this.latch.get().countDown();
            protonReceiver.get().flow(protonSender.get().getCredit() - protonReceiver.get().getCredit());
        });
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setReceiver(ProtonReceiver protonReceiver) {
        this.protonReceiver.set(protonReceiver);
    }

    public void setSender(ProtonSender protonSender) {
        this.protonSender.set(protonSender);
    }

    public void setLatch(CountDownLatch latch) {
        this.latch.set(latch);
    }

    public Endpoint toEndpoint() {
        return toEndpoint;
    }
}
