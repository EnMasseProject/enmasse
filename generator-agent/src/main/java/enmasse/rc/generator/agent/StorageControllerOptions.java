package enmasse.rc.generator.agent;

import com.openshift.restclient.images.DockerImageURI;
import enmasse.rc.model.BrokerProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author lulf
 */
public final class StorageControllerOptions {

    private final String configHost;
    private final int configPort;

    private final String openshiftUrl;
    private final BrokerProperties brokerProperties;

    private StorageControllerOptions(String configHost, int configPort, String openshiftUrl, BrokerProperties brokerProperties) {
        this.configHost = configHost;
        this.configPort = configPort;
        this.openshiftUrl = openshiftUrl;
        this.brokerProperties = brokerProperties;
    }

    public int configPort() {
        return configPort;
    }

    public String configHost() {
        return configHost;
    }

    public String openshiftUrl() {
        return openshiftUrl;
    }

    public BrokerProperties brokerProperties() {
        return brokerProperties;
    }

    public static StorageControllerOptions fromEnv(Map<String, String> env) {
        String openshiftHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String openshiftPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");

        String configHost = getEnvOrThrow(env, "CONFIGURATION_SERVICE_HOST");
        int configPort = Integer.parseInt(getEnvOrThrow(env, "CONFIGURATION_SERVICE_PORT"));

        BrokerProperties brokerProperties = getBrokerProperties(env);

        return new StorageControllerOptions(configHost, configPort, String.format("https://%s:%s", openshiftHost, openshiftPort), brokerProperties);
    }

    private static BrokerProperties getBrokerProperties(Map<String, String> env) {
        BrokerProperties.Builder builder = new BrokerProperties.Builder();
        ifEnvExists(env, "BROKER_IMAGE", uri -> builder.brokerImage(new DockerImageURI(uri)));
        ifEnvExists(env, "BROKER_PORT", port -> builder.brokerPort(Integer.parseInt(port)));
        ifEnvExists(env, "BROKER_MOUNT_PATH", path -> builder.brokerMountPath(path));
        ifEnvExists(env, "ROUTER_IMAGE", uri -> builder.routerImage(new DockerImageURI(uri)));
        ifEnvExists(env, "ROUTER_PORT", port -> builder.brokerPort(Integer.parseInt(port)));
        return builder.build();
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static void ifEnvExists(Map<String, String> env, String envName, Consumer<String> fn) {
        if (env.containsKey(envName)) {
            fn.accept(env.get(envName));
        }
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    public String openshiftNamespace() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
    }

    public String openshiftToken() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
