package quilt.config.generator.service;

import org.apache.qpid.proton.amqp.transport.Open;
import org.junit.Test;
import quilt.config.generator.ConfigGenerator;
import quilt.config.model.Broker;
import quilt.config.model.Config;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author lulf
 */
public class ConfigSubscriberTest {
    @Test
    public void testBrokerAdded() {
        OpenshiftClient mockClient = mock(OpenshiftClient.class);

        ConfigSubscriber subscriber = new ConfigSubscriber(mockClient, new ConfigGenerator(null));
        subscriber.configUpdated(new Config(Collections.singletonList(new Broker("broker1", true, false))));
        verify(mockClient).brokerAdded(any());
    }
}
