package enmasse.rc.generator;

import com.openshift.internal.restclient.model.volume.PersistentVolumeClaim;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import enmasse.rc.model.*;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author lulf
 */
public class BrokerGeneratorTest {
    private BrokerGenerator generator;
    private IPersistentVolumeClaim emptyClaim = new PersistentVolumeClaim(new ModelNode(), null, Collections.emptyMap());

    @Before
    public void setup() {
        generator = new BrokerGenerator(null, new BrokerProperties.Builder().brokerPort(1234).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowOnNoStore() {
        generator.generate(new Destination("testaddr", false, false), emptyClaim);
    }

    @Test
    public void testGenerator() {
        IReplicationController controller = generator.generate(new Destination("testaddr", true, false), emptyClaim);

        assertThat(controller.getName(), is("controller-testaddr"));
        assertThat(controller.getLabels().get(LabelKeys.ROLE), is(Roles.BROKER));
        assertThat(controller.getContainers().size(), is(2));

        IContainer broker = controller.getContainer("broker");
        assertThat(broker.getPorts().size(), is(1));
        assertThat(broker.getPorts().iterator().next().getContainerPort(), is(1234));
        assertThat(broker.getEnvVars().get(EnvVars.QUEUE_NAME), is("testaddr"));
        assertThat(broker.getVolumeMounts().size(), is(1));

        IContainer router = controller.getContainer("router");
        assertThat(router.getPorts().size(), is(1));
        assertThat(router.getPorts().iterator().next().getContainerPort(), is(5672));
    }

    @Test
    public void testGenerateTopic() {
        IReplicationController controller = generator.generate(new Destination("testaddr", true, true), emptyClaim);
        IContainer broker = controller.getContainer("broker");
        assertThat(broker.getEnvVars().get(EnvVars.TOPIC_NAME), is("testaddr"));
    }
}
