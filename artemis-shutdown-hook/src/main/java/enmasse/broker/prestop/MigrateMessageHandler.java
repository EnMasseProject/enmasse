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

import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;

import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO: Description
 */
public class MigrateMessageHandler {
    private final AtomicReference<ProtonReceiver> protonReceiver = new AtomicReference<>();
    private final AtomicReference<ProtonSender> protonSender = new AtomicReference<>();
    private volatile boolean ready = false;
    private final Subscription me;

    public MigrateMessageHandler(String id) {
        this.me = new Subscription(id, id, true);
    }

    public Subscription getSubscription() {
        return me;
    }

    public ProtonMessageHandler messageHandler() {
        return (sourceDelivery, message) -> protonSender.get().send(message, protonDelivery -> {
            System.out.println("Forwarding message to subscriber");
            sourceDelivery.disposition(protonDelivery.getRemoteState(), protonDelivery.remotelySettled());
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
}
