/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.ArgumentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class ClientTestBase extends TestBaseWithShared {
    private final String clientFolder = "clients_tests";
    protected ArgumentMap arguments = new ArgumentMap();
    protected List<AbstractClient> clients;
    protected Path logPath = null;

    @BeforeEach
    public void setUpClientBase(TestInfo info) {
        clients = new ArrayList<>();
        logPath = Paths.get(
                environment.testLogDir(),
                clientFolder,
                info.getTestClass().get().getName(),
                info.getTestMethod().get().getName());
    }

    @AfterEach
    public void teardownClient(ExtensionContext context) {
        arguments.clear();
        clients.forEach(AbstractClient::stop);
        clients.clear();
    }

    protected String getTopicPrefix(boolean topicSwitch) {
        return topicSwitch ? "topic://" : "";
    }

}
