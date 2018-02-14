/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

public class QueueTerminusFactory implements TerminusFactory {
    @Override
    public Source getSource(String address) {
        Source source = new Source();
        source.setAddress(address);
        return source;
    }

    @Override
    public Target getTarget(String address) {
        Target target = new Target();
        target.setAddress(address);
        return target;
    }
}
