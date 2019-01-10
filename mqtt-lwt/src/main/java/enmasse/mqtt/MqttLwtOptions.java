/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class MqttLwtOptions {
    private String messagingServiceHost;
    private int messagingServiceNormalPort;
    private int routeContainerPort;
    private String certDir;
    private Duration startupTimeout;

    public String getMessagingServiceHost() {
        return messagingServiceHost;
    }

    public void setMessagingServiceHost(String messagingServiceHost) {
        this.messagingServiceHost = messagingServiceHost;
    }

    public int getMessagingServiceNormalPort() {
        return messagingServiceNormalPort;
    }

    public void setMessagingServiceNormalPort(int messagingServiceNormalPort) {
        this.messagingServiceNormalPort = messagingServiceNormalPort;
    }

    public int getRouteContainerPort() {
        return routeContainerPort;
    }

    public void setRouteContainerPort(int routeContainerPort) {
        this.routeContainerPort = routeContainerPort;
    }

    public String getCertDir() {
        return certDir;
    }

    public void setCertDir(String certDir) {
        this.certDir = certDir;
    }

    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    public static MqttLwtOptions fromEnv(Map<String, String> env) {

        MqttLwtOptions options = new MqttLwtOptions();

        options.setMessagingServiceHost(getEnv(env, "MESSAGING_SERVICE_HOST")
                .orElse("0.0.0.0"));

        options.setMessagingServiceNormalPort(getEnv(env, "MESSAGING_SERVICE_NORMAL_PORT")
                .map(Integer::parseInt)
                .orElse(5672));

        options.setRouteContainerPort(getEnv(env, "MESSAGING_SERVICE_ROUTE_CONTAINER_PORT")
                .map(Integer::parseInt)
                .orElse(55671));

        options.setStartupTimeout(getEnv(env, "ENMASSE_MQTT_STARTUPTIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5)));

        options.setCertDir(getEnv(env, "CERT_DIR")
                .orElseThrow(() -> new IllegalArgumentException("CERT_DIR is required")));

        return options;
    }

    @Override
    public String toString() {
        return "MqttLwtOptions{" +
                "messagingServiceHost='" + messagingServiceHost + '\'' +
                ", messagingServiceNormalPort=" + messagingServiceNormalPort +
                ", routeContainerPort=" + routeContainerPort +
                ", certDir='" + certDir + '\'' +
                ", startupTimeout=" + startupTimeout +
                '}';
    }
}
