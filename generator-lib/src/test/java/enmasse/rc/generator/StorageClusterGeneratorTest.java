package enmasse.rc.generator;

import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Destination;
import enmasse.rc.openshift.OpenshiftClient;
import enmasse.rc.openshift.StorageCluster;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author lulf
 */
public class StorageClusterGeneratorTest {
    private OpenshiftClient mockClient;

    @Before
    public void setUp() {
        mockClient = mock(OpenshiftClient.class);
    }

    @Test
    public void testSkipNoStore() {
        StorageGenerator generator = new StorageGenerator(mockClient, new BrokerProperties.Builder().build());
        List<StorageCluster> resources = generator.generate(Arrays.asList(new Destination("foo", true, false), new Destination("bar", false, false)));
        assertThat(resources.size(), is(1));
    }

    @Test
    public void testGenerate() {
        BrokerProperties properties = new BrokerProperties.Builder().brokerPort(1234).build();
        StorageGenerator generator = new StorageGenerator(mockClient, properties);
        List<StorageCluster> clusterList = generator.generate(Arrays.asList(new Destination("foo", true, false), new Destination("bar", false, false)));
        assertThat(clusterList.size(), is(1));
        StorageCluster cluster = clusterList.get(0);
        assertThat(cluster.getAddress(), is("foo"));
        assertThat(cluster.getResources().size(), is(2));
        IReplicationController controller = getController(cluster.getResources());
        assertThat(controller.getContainer("broker").getPorts().iterator().next().getContainerPort(), is(1234));
    }

    private IReplicationController getController(List<IResource> resources) {
        return resources.stream()
                .filter(resource -> resource instanceof IReplicationController)
                .map(resource -> (IReplicationController)resource)
                .findAny()
                .get();
    }
}
