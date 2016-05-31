package quilt.config.generator.agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author lulf
 */
public final class GeneratorAgentOptions {

    private final String configHost;
    private final int configPort;

    private final String openshiftHost;
    private final int openshiftPort;

    private GeneratorAgentOptions(String configHost, int configPort, String openshiftHost, int openshiftPort) {
        this.configHost = configHost;
        this.configPort = configPort;
        this.openshiftHost = openshiftHost;
        this.openshiftPort = openshiftPort;
    }

    public int configPort() {
        return configPort;
    }

    public String configHost() {
        return configHost;
    }

    public String openshiftHost() {
        return openshiftHost;
    }

    public int openshiftPort() {
        return openshiftPort;
    }

    public static GeneratorAgentOptions fromUrls(String configUrl, String openshiftUrl) {
        String openshiftHost = parseHost(openshiftUrl);
        int openshiftPort = parsePort(openshiftUrl);

        String configHost = parseHost(configUrl);
        int configPort = parsePort(configUrl);

        return new GeneratorAgentOptions(configHost, configPort, openshiftHost, openshiftPort);
    }

    private static String parseHost(String url) {
        StringBuilder builder = new StringBuilder();
        String parts[] = url.split(":");
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                builder.append(":");
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    private static int parsePort(String url) {
        String parts[] = url.split(":");
        return Integer.parseInt(parts[parts.length - 1]);
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
