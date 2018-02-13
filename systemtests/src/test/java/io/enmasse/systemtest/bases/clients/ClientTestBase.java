/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.ArgumentMap;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;


public abstract class ClientTestBase extends TestBaseWithShared {
    protected ArgumentMap arguments = new ArgumentMap();
    protected List<AbstractClient> clients;

    @Before
    public void setUpClientBase() {
        clients = new ArrayList<>();
    }

    @After
    public void teardownClient() {
        arguments.clear();
        clients.forEach(AbstractClient::stop);
        clients.clear();
    }

    protected String getTopicPrefix(boolean topicSwitch) {
        return topicSwitch ? "topic://" : "";
    }
}
