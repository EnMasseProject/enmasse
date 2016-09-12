package enmasse.config.bridge.model;

import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class ConfigMapTest {
    @Test
    public void testSubscribing() {
        ConfigMap map = new ConfigMap("mymap");
        ConfigSubscriber mockSub = mock(ConfigSubscriber.class);
        map.subscribe(mockSub);
        map.configUpdated("1234", Collections.singletonMap("foo", "bar"));
    }
}
