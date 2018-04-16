/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.amqp;

import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;

import java.util.Optional;

public class LinkOptions {
    private final Source source;
    private final Target target;
    private final Optional<String> linkName;

    public LinkOptions(Source source, Target target, Optional<String> linkName) {
        this.source = source;
        this.target = target;
        this.linkName = linkName;
    }

    public Source getSource() {
        return source;
    }

    public Target getTarget() {
        return target;
    }

    public Optional<String> getLinkName() {
        return linkName;
    }
}
