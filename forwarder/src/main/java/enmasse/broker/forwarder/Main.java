package enmasse.broker.forwarder;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import enmasse.discovery.DiscoveryClient;
import enmasse.discovery.Host;
import io.vertx.core.impl.FileResolver;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ulf Lilleengen
 */
public class Main {

    public static void main(String [] args) throws IOException, InterruptedException {
        System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x");
        Logger.getGlobal().setLevel(Level.INFO);

        Map<String, String> env = System.getenv();
        IClient client = createClient(env);
        String namespace = openshiftNamespace();
        Map<String, String> labelFilter = getLabelFilter(env);
        Host localHost = getLocalHost();
        String address = getAddress(env);
        String containerId = getContainerId(env);
        
        DiscoveryClient discoveryClient = new DiscoveryClient(client, namespace, labelFilter);
        ForwarderController replicator = new ForwarderController(localHost, address, containerId);

        discoveryClient.addListener(replicator);

        discoveryClient.start();
    }

    private static String getContainerId(Map<String, String> env) {
        return getEnvOrThrow(env, "CONTAINER_ID");
    }

    private static IClient createClient(Map<String, String> env) throws IOException {
        String openshiftHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String openshiftPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");
        return new ClientBuilder(String.format("https://%s:%s", openshiftHost, openshiftPort))
                .authorizationStrategy(new TokenAuthorizationStrategy(openshiftToken()))
                .build();
    }

    private static String getAddress(Map<String, String> env) {
        return getEnvOrThrow(env, "TOPIC_NAME");
    }

    private static Host getLocalHost() throws UnknownHostException {
        return new Host(Inet4Address.getLocalHost().getHostAddress(), Collections.singletonMap("amqp", 5673));
    }

    private static Map<String,String> getLabelFilter(Map<String, String> env) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("role", "broker");
        labelMap.put("address", getAddress(env));
        return labelMap;
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private static String openshiftNamespace() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
    }

    private static String openshiftToken() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

}
