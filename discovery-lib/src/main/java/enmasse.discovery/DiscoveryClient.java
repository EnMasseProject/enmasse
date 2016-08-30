package enmasse.discovery;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IResource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ulf Lilleengen
 */
public class DiscoveryClient implements IOpenShiftWatchListener {
    private final IClient osClient;
    private final String namespace;
    private final Map<String, String> labelFilter;
    private volatile IWatcher watch;
    private final List<DiscoveryListener> listeners = new ArrayList<>();

    public DiscoveryClient(IClient osClient, String namespace, Map<String, String> labelFilter) {
        this.osClient = osClient;
        this.namespace = namespace;
        this.labelFilter = labelFilter;
    }

    public void addListener(DiscoveryListener listener) {
        this.listeners.add(listener);
    }

    public void start() {
        this.watch = osClient.watch(namespace, this, ResourceKind.POD);
    }

    public void stop() {
        if (watch != null) {
            watch.stop();
        }
    }

    @Override
    public void connected(List<IResource> resources) {
        Set<Host> hosts = resources.stream()
                .filter(r -> filterLabels(r.getLabels()))
                .map(r -> podToHost((IPod)r))
                .collect(Collectors.toSet());

        notifyListeners(hosts);
    }

    private boolean filterLabels(Map<String, String> labels) {
        for (Map.Entry<String, String> entrySet : labelFilter.entrySet()) {
            if (!labels.containsKey(entrySet.getKey()) || !labels.get(entrySet.getKey()).equals(entrySet.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void notifyListeners(Set<Host> hosts) {
        for (DiscoveryListener listener : listeners) {
            listener.hostsChanged(hosts);
        }
    }

    private static final Host podToHost(IPod pod) {
        return new Host(pod.getHost(), createPortMap(pod.getContainerPorts()));
    }

    private static Map<String, Integer> createPortMap(Set<IPort> containerPorts) {
        Map<String, Integer> portMap = new LinkedHashMap<>();
        for (IPort port : containerPorts) {
            if (port.getName() == null) {
                portMap.put(String.valueOf(port.getContainerPort()), port.getContainerPort());
            } else {
                portMap.put(port.getName(), port.getContainerPort());
            }
        }
        return portMap;
    }

    @Override
    public void disconnected() {
        this.watch = osClient.watch(namespace, this, ResourceKind.POD);
    }

    private final Set<Host> fetchHosts() {
        return osClient.<IPod>list(ResourceKind.POD, namespace, labelFilter).stream()
                .map(DiscoveryClient::podToHost)
                .collect(Collectors.toSet());
    }

    @Override
    public void received(IResource resource, ChangeType change) {
        if (labelFilter.equals(resource.getLabels())) {
            notifyListeners(fetchHosts());
        }
    }

    @Override
    public void error(Throwable err) {

    }
}
