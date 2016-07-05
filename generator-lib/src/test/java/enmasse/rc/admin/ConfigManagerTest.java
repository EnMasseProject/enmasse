package enmasse.rc.admin;

import org.junit.Test;
import enmasse.rc.generator.StorageGenerator;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.openshift.OpenshiftClient;
import enmasse.rc.model.Destination;
import enmasse.rc.model.Config;
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
