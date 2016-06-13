package enmasse.rc.admin;

import org.junit.Test;
import enmasse.rc.generator.ConfigGenerator;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.openshift.OpenshiftClient;
import enmasse.rc.model.Destination;
import enmasse.rc.model.Config;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author lulf
 */
public class ConfigManagerTest {
    @Test
    public void testBrokerAdded() {
        OpenshiftClient mockClient = mock(OpenshiftClient.class);

        BrokerProperties props = new BrokerProperties.Builder()
                .brokerPort(1234)
                .build();
        ConfigManager manager = new ConfigManager(mockClient, new ConfigGenerator(null, props));
        manager.configUpdated(new Config(Arrays.asList(new Destination("broker1", true, false), new Destination("broker2", false, false))));
        verify(mockClient).createBroker(any());
    }
}
