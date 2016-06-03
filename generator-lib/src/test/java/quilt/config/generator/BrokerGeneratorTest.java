package quilt.config.generator;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IReplicationController;
import org.junit.Before;
import org.junit.Test;
import quilt.config.model.Destination;
import quilt.config.model.EnvVars;
import quilt.config.model.LabelKeys;
import quilt.config.model.Roles;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author lulf
 */
public class BrokerGeneratorTest {
    private BrokerGenerator generator;

    @Before
    public void setup() {
        generator = new BrokerGenerator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowOnNoStore() {
        generator.generate(new Destination("testaddr", false, false));
    }

    @Test
    public void testGenerator() {
        IReplicationController controller = generator.generate(new Destination("testaddr", true, false));

        assertThat(controller.getName(), is("controller-testaddr"));
        assertThat(controller.getLabels().get(LabelKeys.ROLE), is(Roles.BROKER));
        assertThat(controller.getContainers().size(), is(2));

        IContainer broker = controller.getContainer("broker");
        assertThat(broker.getPorts().size(), is(1));
        assertThat(broker.getPorts().iterator().next().getContainerPort(), is(5673));
        assertThat(broker.getEnvVars().get(EnvVars.QUEUE_NAME), is("testaddr"));
        assertThat(broker.getVolumeMounts().size(), is(1));

        IContainer router = controller.getContainer("router");
        assertThat(router.getPorts().size(), is(1));
        assertThat(router.getPorts().iterator().next().getContainerPort(), is(5672));
    }

    @Test
    public void testGenerateTopic() {
        IReplicationController controller = generator.generate(new Destination("testaddr", true, true));
        IContainer broker = controller.getContainer("broker");
        assertThat(broker.getEnvVars().get(EnvVars.TOPIC_NAME), is("testaddr"));
    }
}
