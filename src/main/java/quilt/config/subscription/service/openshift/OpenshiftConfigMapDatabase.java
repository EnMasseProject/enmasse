package quilt.config.subscription.service.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IConfigMap;
import com.openshift.restclient.model.IResource;
import quilt.config.subscription.service.model.ConfigMap;
import quilt.config.subscription.service.model.ConfigMapDatabase;
import quilt.config.subscription.service.model.ConfigSubscriber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ConfigMapDatabase backed by OpenShift/Kubernetes REST API supporting subscriptions.
 *
 * @author lulf
 */
public class OpenshiftConfigMapDatabase implements IOpenShiftWatchListener, AutoCloseable, ConfigMapDatabase {
    private static final Logger log = Logger.getLogger(OpenshiftConfigMapDatabase.class.getName());
    private final IClient restClient;
    private final String namespace;
    private IWatcher watcher = null;

    private final Map<String, ConfigMap> configMapMap = new ConcurrentHashMap<>();

    public OpenshiftConfigMapDatabase(IClient restClient, String namespace) {
        this.restClient = restClient;
        this.namespace = namespace;
    }

    public void start() {
        log.log(Level.INFO, "Starting to watch configs");
        this.watcher = restClient.watch(namespace, this, ResourceKind.CONFIG_MAP);
    }

    @Override
    public void connected(List<IResource> resources) {
        log.log(Level.INFO, "Connected, got " + resources.size() + " resources");
        for (IResource resource : resources) {
            IConfigMap configMap = (IConfigMap) resource;

            ConfigMap map = getOrCreateConfigMap(configMap.getName());
            map.configUpdated(configMap.getResourceVersion(), configMap.getData());
            log.log(Level.FINE, "Added config map '" + configMap.getName() + "'");
        }
    }

    @Override
    public void disconnected() {
        log.log(Level.INFO, "Disconnected, restarting watch");
        try {
            this.watcher = restClient.watch(namespace, this, ResourceKind.CONFIG_MAP);
        } catch (Exception e) {
            log.log(Level.INFO, "Error re-watching on disconnect from " + restClient.getBaseURL(), e);
        }
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        IConfigMap configMap = (IConfigMap) resource;
        if (change.equals(ChangeType.DELETED)) {
            // TODO: Notify subscribers?
            configMapMap.remove(configMap.getName());
            log.log(Level.FINE, "ConfigMap " + configMap.getName() + " deleted!");
        } else if (change.equals(ChangeType.ADDED)) {
            ConfigMap map = getOrCreateConfigMap(configMap.getName());
            map.configUpdated(configMap.getResourceVersion(), configMap.getData());
            log.log(Level.FINE, "ConfigMap " + configMap.getName() + " added!");
        } else if (change.equals(ChangeType.MODIFIED)) {
            // TODO: Can we assume that it exists at this point?
            configMapMap.get(configMap.getName()).configUpdated(configMap.getResourceVersion(), configMap.getData());
            log.log(Level.FINE, "ConfigMap " + configMap.getName() + " updated!");
        }
    }

    @Override
    public void error(Throwable err) {
        log.log(Level.WARNING, "Error when watching: " + err.getMessage());
    }

    @Override
    public void close() throws Exception {
        if (this.watcher != null) {
            watcher.stop();
        }
    }

    /**
     * This method is synchronized so that we can support atomically getOrCreate on top of the configMapMap.
     */
    private synchronized ConfigMap getOrCreateConfigMap(String name)
    {
        ConfigMap map = configMapMap.get(name);
        if (map == null) {
            map = new ConfigMap(name);
            configMapMap.put(name, map);
        }
        return map;
    }

    public void subscribe(String name, ConfigSubscriber configSubscriber) {
        ConfigMap configMap = getOrCreateConfigMap(name);
        configMap.subscribe(configSubscriber);
    }
}
