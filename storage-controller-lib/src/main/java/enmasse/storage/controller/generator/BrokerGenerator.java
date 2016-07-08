package enmasse.storage.controller.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.Port;
import com.openshift.internal.restclient.model.ReplicationController;
import com.openshift.internal.restclient.model.volume.SecretVolumeSource;
import com.openshift.internal.restclient.model.volume.VolumeMount;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.ISecretVolumeSource;
import com.openshift.restclient.model.volume.IVolumeMount;
import com.openshift.restclient.model.volume.IVolumeSource;
import enmasse.storage.controller.model.*;
import org.jboss.dmr.ModelNode;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class BrokerGenerator {
    private static final Logger log = Logger.getLogger(BrokerGenerator.class.getName());
    private final ResourceFactory factory;

    public BrokerGenerator(IClient osClient) {
        this.factory = new ResourceFactory(osClient);
    }

    public IReplicationController generate(Destination destination, IVolumeSource volumeSource) {
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
        controller.addVolume(volumeSource);

        generateBroker(controller, destination, volumeSource);
        generateDispatchRouter(controller, destination);

        return controller;
    }

    private void generateBroker(ReplicationController controller, Destination destination, IVolumeSource volumeSource) {
        FlavorConfig flavorConfig = destination.flavor();
        Port amqpPort = new Port(new ModelNode());
        amqpPort.setContainerPort(flavorConfig.brokerPort());
        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        env.put(EnvVars.BROKER_PORT, String.valueOf(flavorConfig.brokerPort()));

        IContainer broker = controller.addContainer(
                "broker",
                flavorConfig.brokerImage(),
                Collections.singleton(amqpPort),
                env,
                Collections.emptyList());

        IVolumeMount volumeMount = new VolumeMount(new ModelNode());
        volumeMount.setName(volumeSource.getName());
        volumeMount.setReadOnly(false);
        volumeMount.setMountPath(flavorConfig.storageConfig().mountPath());
        broker.setVolumeMounts(Collections.singleton(volumeMount));
    }

    private void generateDispatchRouter(ReplicationController controller, Destination destination) {
        FlavorConfig flavorConfig = destination.flavor();
        Port interRouterPort = new Port(new ModelNode());
        interRouterPort.setContainerPort(flavorConfig.routerPort());

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        env.put(EnvVars.INTERROUTER_PORT, String.valueOf(flavorConfig.routerPort()));
        IContainer router = controller.addContainer(
                "router",
                flavorConfig.routerImage(),
                Collections.singleton(interRouterPort),
                env,
                Collections.emptyList());


        if (flavorConfig.routerSecretName() != null) {
            IVolumeMount volumeMount = new IVolumeMount() {
                    public String getName() { return "ssl-certs"; }
                    public String getMountPath() { return flavorConfig.routerSecretPath(); }
                    public boolean isReadOnly() { return true; }
                    public void setName(String name) {}
                    public void setMountPath(String path) {}
                    public void setReadOnly(boolean readonly) {}
                };
            Set<IVolumeMount> mounts = new HashSet<>();
            mounts.add(volumeMount);
            router.setVolumeMounts(mounts);
            ISecretVolumeSource volumeSource = new SecretVolumeSource("ssl-certs");
            volumeSource.setSecretName(flavorConfig.routerSecretName());
            controller.addVolume(volumeSource);
        }
    }
}
