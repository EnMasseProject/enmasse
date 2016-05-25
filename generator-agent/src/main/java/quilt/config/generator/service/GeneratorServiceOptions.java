package quilt.config.generator.service;

import java.util.IntSummaryStatistics;

/**
 * @author lulf
 */
public final class GeneratorServiceOptions {

    private final String configHost;
    private final int configPort;

    private final String openshiftHost;
    private final int openshiftPort;

    private final String namespace;
    private final String token;
    private final String user;

    private GeneratorServiceOptions(String configHost, int configPort, String openshiftHost, int openshiftPort, String namespace, String token, String user) {
        this.configHost = configHost;
        this.configPort = configPort;
        this.openshiftHost = openshiftHost;
        this.openshiftPort = openshiftPort;
        this.namespace = namespace;
        this.token = token;
        this.user = user;
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

    public static GeneratorServiceOptions create(String configUrl, String openshiftUrl, String namespace, String token, String user) {
        String openshiftHost = parseHost(openshiftUrl);
        int openshiftPort = parsePort(openshiftUrl);

        String configHost = parseHost(configUrl);
        int configPort = parsePort(configUrl);

        return new GeneratorServiceOptions(configHost, configPort, openshiftHost, openshiftPort, namespace, token, user);
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

    public String openshiftUser() {
        return user;
    }

    public String openshiftToken() {
        return token;
    }

    public String openshiftNamespace() {
        return namespace;
    }
}
