package openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IConfigMap;
import quilt.config.subscription.service.model.ConfigSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import quilt.config.subscription.service.openshift.OpenshiftConfigMapDatabase;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * @author lulf
 */
public class OpenshiftConfigMapDatabaseTest {
    private OpenshiftConfigMapDatabase database;

    @Before
    public void setup() {
        IClient client = mock(IClient.class);
        database = new OpenshiftConfigMapDatabase(client, "testspace");
        database.start();
        verify(client).watch("testspace", database, ResourceKind.CONFIG_MAP);
    }

    @After
    public void teardown() throws Exception {
        database.close();
    }

    @Test
    public void testSubscribeBeforeConnected() {
        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        database.subscribe("foo", sub);

        verifyZeroInteractions(sub);

        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        verify(sub).configUpdated("foo", "1234", testValue);
    }

    @Test
    public void testSubscribeAfterConnected() {

        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        database.subscribe("foo", sub);

        verify(sub).configUpdated("foo", "1234", testValue);
    }

    @Test
    public void testUpdates() {
        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        database.subscribe("foo", sub);

        verify(sub).configUpdated("foo", "1234", testValue);

        testValue = Collections.singletonMap("quux", "bim");
        IConfigMap newMap = mock(IConfigMap.class);
        when(newMap.getName()).thenReturn("foo");
        when(newMap.getResourceVersion()).thenReturn("1235");
        when(newMap.getData()).thenReturn(testValue);
        database.received(newMap, IOpenShiftWatchListener.ChangeType.MODIFIED);

        verify(sub).configUpdated("foo", "1235", testValue);
    }

    private void connectWithValues(Map<String, String> testValue) {
        IConfigMap testMap = mock(IConfigMap.class);
        when(testMap.getName()).thenReturn("foo");
        when(testMap.getResourceVersion()).thenReturn("1234");
        when(testMap.getData()).thenReturn(testValue);
        database.connected(Collections.singletonList(testMap));
    }
}
