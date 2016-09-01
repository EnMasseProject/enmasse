package enmasse.storage.controller.generator;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.internal.restclient.model.*;
import com.openshift.internal.restclient.model.Port;
import com.openshift.internal.restclient.model.volume.SecretVolumeSource;
import com.openshift.internal.restclient.model.volume.VolumeMount;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.volume.ISecretVolumeSource;
import com.openshift.restclient.model.volume.IVolumeMount;
import com.openshift.restclient.model.volume.IVolumeSource;
import enmasse.storage.controller.model.*;
import org.jboss.dmr.ModelNode;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        controller.addTemplateLabel(LabelKeys.ADDRESS, destination.address());
        controller.setReplicaSelector(Collections.singletonMap(LabelKeys.ADDRESS, destination.address()));
        controller.addVolume(volumeSource);

        generateBroker(controller, destination, volumeSource);
        generateDispatchRouter(controller, destination);
        if (destination.multicast()) {
            generateForwarder(controller, destination);
        }

        return controller;
    }

    private void generateForwarder(ReplicationController controller, Destination destination) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put(EnvVars.TOPIC_NAME, destination.address());
        controller.addContainer(
                "forwarder",
                new DockerImageURI("enmasseproject/topic-forwarder:latest"),
                Collections.emptySet(),
                env,
                Collections.emptyList());
    }

    private void generateBroker(ReplicationController controller, Destination destination, IVolumeSource volumeSource) {
        FlavorConfig flavorConfig = destination.flavor();

        Set<IPort> ports = flavorConfig.brokerPorts().stream()
                .map(port -> createPort(port.name(), port.port()))
                .collect(Collectors.toSet());

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());

        IContainer broker = controller.addContainer(
                "broker",
                flavorConfig.brokerImage(),
                ports,
                env,
                Collections.emptyList());

        IVolumeMount volumeMount = new VolumeMount(new ModelNode());
        volumeMount.setName(volumeSource.getName());
        volumeMount.setReadOnly(false);
        volumeMount.setMountPath(flavorConfig.storageConfig().mountPath());
        broker.setVolumeMounts(Collections.singleton(volumeMount));

        if (!destination.multicast()) {
            flavorConfig.shutdownHook().ifPresent(hook -> {
                broker.setLifecycle(new Lifecycle.Builder()
                        .preStop(new ExecAction.Builder()
                                .command(hook)
                                .build())
                        .build());
            });
        }
    }

    private static IPort createPort(String portName, int port) {
        Port portModel = new Port(new ModelNode());
        portModel.setName(portName);
        portModel.setContainerPort(port);
        return portModel;
    }

    private void generateDispatchRouter(ReplicationController controller, Destination destination) {
        FlavorConfig flavorConfig = destination.flavor();
        Set<IPort> ports = flavorConfig.routerPorts().stream()
                .map(port -> createPort(port.name(), port.port()))
                .collect(Collectors.toSet());

        Map<String, String> env = new LinkedHashMap<>();
        env.put(destination.multicast() ? EnvVars.TOPIC_NAME : EnvVars.QUEUE_NAME, destination.address());

        IContainer router = controller.addContainer(
                "router",
                flavorConfig.routerImage(),
                ports,
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
