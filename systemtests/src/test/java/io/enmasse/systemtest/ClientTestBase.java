package io.enmasse.systemtest;

import io.enmasse.systemtest.executor.client.ArgumentMap;
import org.junit.After;


public class ClientTestBase extends MultiTenantTestBase {
    protected ArgumentMap arguments = new ArgumentMap();

    @After
    public void teardownClient(){
        arguments.clear();
    }
}
