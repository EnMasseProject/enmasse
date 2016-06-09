package quilt.config.subscription.service;

import com.openshift.restclient.ClientBuilder;
import com.openshift.restclient.IClient;
import com.openshift.restclient.authorization.TokenAuthorizationStrategy;
import io.vertx.core.impl.FileResolver;
import quilt.config.subscription.service.amqp.AMQPServer;
import quilt.config.subscription.service.openshift.OpenshiftConfigMapDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main entrypoint for configuration service with arg parsing.
 *
 * @author lulf
 */
public class Main {

    public static void main(String [] args) {
        try {
            // Kind of a hack
            System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x");

            Map<String, String> env = System.getenv();
            String openshiftUri = String.format("https://%s:%s", getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST"), getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT"));
            String listenAddress = env.getOrDefault("CONFIGURATION_SERVICE_LISTEN_ADDRESS", "0.0.0.0");
            int listenPort = Integer.parseInt(env.getOrDefault("CONFIGURATION_SERVICE_LISTEN_PORT", "5672"));

            String openshiftNamespace = getOpenshiftNamespace();

            IClient client = new ClientBuilder(openshiftUri)
                    .authorizationStrategy(new TokenAuthorizationStrategy(getAuthenticationToken()))
                    .build();

            OpenshiftConfigMapDatabase database = new OpenshiftConfigMapDatabase(client, openshiftNamespace);
            database.start();

            AMQPServer server = new AMQPServer(listenAddress, listenPort, database);

            server.run();
        } catch (IllegalArgumentException e) {
            System.out.println("Error parsing environment: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error running config subscription service");
            System.exit(1);
        }
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private static String getOpenshiftNamespace() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
    }

    private static String getAuthenticationToken() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
