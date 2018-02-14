/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

public class TopicTerminusFactory extends QueueTerminusFactory {
    @Override
    public Source getSource(String address) {
        Source source = super.getSource(address);
        source.setCapabilities(Symbol.getSymbol("topic"));
        return source;
    }

    @Override
    public Target getTarget(String address) {
        Target target = super.getTarget(address);
        target.setCapabilities(Symbol.getSymbol("topic"));
        return target;
    }
}
