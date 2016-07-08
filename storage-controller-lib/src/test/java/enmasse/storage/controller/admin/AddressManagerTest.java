package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.FlavorConfig;
import enmasse.storage.controller.model.AddressConfig;
import org.junit.Test;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.model.Destination;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author lulf
 */
public class AddressManagerTest {
    @Test
    public void testBrokerAdded() {
        OpenshiftClient mockClient = mock(OpenshiftClient.class);

        FlavorConfig props = new FlavorConfig.Builder()
                .brokerPort(1234)
                .build();
        AddressManager manager = new AddressManager(mockClient, new StorageGenerator(mockClient), null);
        manager.configUpdated(new AddressConfig(Arrays.asList(new Destination("broker1", true, false, props), new Destination("broker2", false, false, props))));
        verify(mockClient, VerificationModeFactory.atLeast(1)).createResource(any());
    }
}
