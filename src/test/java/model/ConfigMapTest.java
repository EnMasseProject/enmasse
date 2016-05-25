package model;

import org.junit.Test;
import quilt.config.subscription.service.model.ConfigMap;
import quilt.config.subscription.service.model.ConfigSubscriber;

import java.util.Collections;

import static org.mockito.Mockito.mock;

/**
 * @author lulf
 */
public class ConfigMapTest {
    @Test
    public void testSubscribing() {
        ConfigMap map = new ConfigMap("mymap");
        ConfigSubscriber mockSub = mock(ConfigSubscriber.class);
        map.subscribe(mockSub);
        map.configUpdated("1234", Collections.singletonMap("foo", "bar"));
    }
}
