package quilt.config.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.Port;
import com.openshift.internal.restclient.model.ReplicationController;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IReplicationController;
import quilt.config.model.Destination;
import org.jboss.dmr.ModelNode;
import quilt.config.model.EnvVars;
import quilt.config.model.LabelKeys;
import quilt.config.model.Roles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class BrokerGenerator {
    private static final Logger log = Logger.getLogger(BrokerGenerator.class.getName());
    private final ResourceFactory factory;

    public BrokerGenerator(IClient osClient) {
        factory = new ResourceFactory(osClient);
    }

    public IReplicationController generate(Destination destination) {
        if (!destination.storeAndForward()) {
            throw new IllegalArgumentException("Not generating broker for destination, storeAndForward = " + destination.storeAndForward());
        }

        ReplicationController controller = factory.create("v1", ResourceKind.REPLICATION_CONTROLLER);
        // TODO: sanitize address
        controller.setName("controller-" + destination.address());
        controller.setReplicas(1);
        controller.addLabel(LabelKeys.ROLE, Roles.BROKER);
        controller.addLabel(LabelKeys.ADDRESS, destination.address());
        controller.addTemplateLabel(LabelKeys.ROLE, Roles.BROKER);
        controller.setReplicaSelector(Collections.singletonMap(LabelKeys.ROLE, Roles.BROKER));

        generateBroker(controller, destination);
        generateDispatchRouter(controller, destination);

        return controller;
    }

    private void generateBroker(ReplicationController controller, Destination destination) {

        Port amqpPort = new Port(new ModelNode());
        amqpPort.setContainerPort(5673);
        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());

        controller.addContainer(
                "broker",
                new DockerImageURI("gordons/artemis:latest"),
                Collections.singleton(amqpPort),
                env,
                Collections.singletonList("/var/run/artemis"));
    }

    private void generateDispatchRouter(ReplicationController controller, Destination destination) {
        Port interRouterPort = new Port(new ModelNode());
        interRouterPort.setContainerPort(5672);

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        controller.addContainer(
                "router",
                new DockerImageURI("gordons/qdrouterd:v7"),
                Collections.singleton(interRouterPort),
                env,
                Collections.emptyList());
    }
}
