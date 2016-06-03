package quilt.config.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.Port;
import com.openshift.internal.restclient.model.ReplicationController;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IReplicationController;
import quilt.config.model.BrokerProperties;
import quilt.config.model.Destination;
import org.jboss.dmr.ModelNode;
import quilt.config.model.Capabilities;
import quilt.config.model.EnvVars;
import quilt.config.model.LabelKeys;
import quilt.config.model.Roles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class BrokerGenerator {
    private static final Logger log = Logger.getLogger(BrokerGenerator.class.getName());
    private final ResourceFactory factory;
    private final BrokerProperties properties;

    public BrokerGenerator(IClient osClient, BrokerProperties properties) {
        this.factory = new ResourceFactory(osClient);
        this.properties = properties;
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
        controller.addTemplateLabel(LabelKeys.CAPABILITY, Capabilities.ROUTER);
        controller.setReplicaSelector(Collections.singletonMap(LabelKeys.ADDRESS, destination.address()));

        generateBroker(controller, destination);
        generateDispatchRouter(controller, destination);

        return controller;
    }

    private void generateBroker(ReplicationController controller, Destination destination) {

        Port amqpPort = new Port(new ModelNode());
        amqpPort.setContainerPort(properties.brokerPort());
        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());

        controller.addContainer(
                "broker",
                properties.brokerImage(),
                Collections.singleton(amqpPort),
                env,
                properties.brokerMounts());
    }

    private void generateDispatchRouter(ReplicationController controller, Destination destination) {
        Port interRouterPort = new Port(new ModelNode());
        interRouterPort.setContainerPort(properties.routerPort());

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        controller.addContainer(
                "router",
                properties.routerImage(),
                Collections.singleton(interRouterPort),
                env,
                Collections.emptyList());
    }
}
