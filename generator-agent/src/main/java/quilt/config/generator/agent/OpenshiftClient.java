package quilt.config.generator.agent;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IReplicationController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lulf
 */
public class OpenshiftClient {
    private static final Logger log = Logger.getLogger(OpenshiftClient.class.getName());
    private final IClient client;
    private final String namespace;

    public OpenshiftClient(IClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    public void brokerAdded(IReplicationController controller) {
        log.log(Level.INFO, "Adding controller " + controller.getName());
        client.create(controller, namespace);
    }

    public void brokerDeleted(IReplicationController controller) {
        log.log(Level.INFO, "Deleting controller " + controller.getName());
        client.delete(controller);
    }

    public void brokerModified(IReplicationController controller) {
        log.log(Level.INFO, "Updating controller " + controller.getName());
        IReplicationController oldController = client.get(ResourceKind.REPLICATION_CONTROLLER, controller.getName(), namespace);
        oldController.setContainers(controller.getContainers());
        oldController.setReplicas(controller.getReplicas());
        oldController.setReplicaSelector(controller.getReplicaSelector());

        for (Map.Entry<String, String> label : controller.getLabels().entrySet()) {
            oldController.addLabel(label.getKey(), label.getValue());
        }

        client.update(oldController);
    }

    public List<IReplicationController> listBrokers() {
        return client.list(ResourceKind.REPLICATION_CONTROLLER, namespace, Collections.singletonMap("role", "broker"));
    }
}
