package quilt.config.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IReplicationController;
import quilt.config.model.LabelKeys;
import quilt.config.model.Roles;

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

    public void createBroker(IReplicationController controller) {
        log.log(Level.INFO, "Adding controller " + controller.getName());
        client.create(controller, namespace);
    }

    public void deleteBroker(IReplicationController controller) {
        log.log(Level.INFO, "Deleting controller " + controller.getName());
        client.delete(controller);
    }

    public void updateBroker(IReplicationController controller) {
        log.log(Level.INFO, "Updating controller " + controller.getName());
        client.update(controller);
    }

    public List<IReplicationController> listBrokers() {
        return client.list(ResourceKind.REPLICATION_CONTROLLER, namespace, Collections.singletonMap(LabelKeys.ROLE, Roles.BROKER));
    }

    public IReplicationController getBroker(String name) {
        return client.get(ResourceKind.REPLICATION_CONTROLLER, name, namespace);
    }
}
