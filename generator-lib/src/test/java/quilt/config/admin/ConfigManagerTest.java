package quilt.config.admin;

import org.junit.Test;
import quilt.config.generator.ConfigGenerator;
import quilt.config.model.BrokerProperties;
import quilt.config.openshift.OpenshiftClient;
import quilt.config.model.Destination;
import quilt.config.model.Config;

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
        manager.configUpdated(new Config(Collections.singletonList(new Destination("broker1", true, false))));
        verify(mockClient).createBroker(any());
    }
}
