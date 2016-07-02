package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.BrokerProperties;
import enmasse.storage.controller.model.Config;
import org.junit.Test;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.model.Destination;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.Arrays;

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
        ConfigManager manager = new ConfigManager(mockClient, new StorageGenerator(mockClient, props));
        manager.configUpdated(new Config(Arrays.asList(new Destination("broker1", true, false), new Destination("broker2", false, false))));
        verify(mockClient, VerificationModeFactory.atLeast(2)).createResource(any());
    }
}
