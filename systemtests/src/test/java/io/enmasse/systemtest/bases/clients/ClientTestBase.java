package io.enmasse.systemtest.bases.clients;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.ArgumentMap;
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
