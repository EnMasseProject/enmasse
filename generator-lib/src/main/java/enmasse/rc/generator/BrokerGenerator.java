package enmasse.rc.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.Port;
import com.openshift.internal.restclient.model.ReplicationController;
import com.openshift.internal.restclient.model.volume.PersistentVolumeClaimVolumeSource;
import com.openshift.internal.restclient.model.volume.VolumeMount;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;
import com.openshift.restclient.model.volume.IPersistentVolumeClaimVolumeSource;
import com.openshift.restclient.model.volume.IVolumeMount;
import enmasse.rc.model.BrokerProperties;
import enmasse.rc.model.Destination;
import enmasse.rc.openshift.OpenshiftClient;
import org.jboss.dmr.ModelNode;
import enmasse.rc.model.Capabilities;
import enmasse.rc.model.EnvVars;
import enmasse.rc.model.LabelKeys;
import enmasse.rc.model.Roles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    public IReplicationController generate(Destination destination, IPersistentVolumeClaim volumeClaim) {
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

        IPersistentVolumeClaimVolumeSource volumeSource = new PersistentVolumeClaimVolumeSource("data-" + destination.address());
        volumeSource.setClaimName(volumeClaim.getName());
        volumeSource.setReadOnly(false);
        controller.addVolume(volumeSource);

        generateBroker(controller, destination, volumeSource);
        generateDispatchRouter(controller, destination);

        return controller;
    }

    private void generateBroker(ReplicationController controller, Destination destination, IPersistentVolumeClaimVolumeSource volumeSource) {

        Port amqpPort = new Port(new ModelNode());
        amqpPort.setContainerPort(properties.brokerPort());
        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        env.put(EnvVars.BROKER_PORT, String.valueOf(properties.brokerPort()));

        IContainer broker = controller.addContainer(
                "broker",
                properties.brokerImage(),
                Collections.singleton(amqpPort),
                env,
                Collections.emptyList());

        IVolumeMount volumeMount = new VolumeMount(new ModelNode());
        volumeMount.setName(volumeSource.getName());
        volumeMount.setReadOnly(false);
        volumeMount.setMountPath(properties.brokerMountPath());
        broker.setVolumeMounts(Collections.singleton(volumeMount));
    }

    private void generateDispatchRouter(ReplicationController controller, Destination destination) {
        Port interRouterPort = new Port(new ModelNode());
        interRouterPort.setContainerPort(properties.routerPort());

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());
        env.put(EnvVars.INTERROUTER_PORT, String.valueOf(properties.routerPort()));
        IContainer router = controller.addContainer(
                "router",
                properties.routerImage(),
                Collections.singleton(interRouterPort),
                env,
                Collections.emptyList());


        if (properties.routerSecretName() != null) {
            IVolumeMount volumeMount = new IVolumeMount() {
                    public String getName() { return "ssl-certs"; }
                    public String getMountPath() { return properties.routerSecretPath(); }
                    public boolean isReadOnly() { return true; }
                    public void setName(String name) {}
                    public void setMountPath(String path) {}
                    public void setReadOnly(boolean readonly) {}
                };
            Set<IVolumeMount> mounts = new HashSet<>();
            mounts.add(volumeMount);
            router.setVolumeMounts(mounts);
            controller.addSecretVolumeToPodSpec(volumeMount, properties.routerSecretName());
        }
    }
}
