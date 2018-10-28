/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class MqttGatewayOptions {

    // binding info for listening
    private String bindAddress;
    private int listenPort;
    // mqtt server options
    private int maxMessageSize;
    // connection info to the messaging service
    private String messagingServiceHost;
    private int messagingServicePort;

    // SSL/TLS support stuff
    private boolean ssl;
    private String certFile;
    private String keyFile;

    private Duration startupTimeout;


    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public String getMessagingServiceHost() {
        return messagingServiceHost;
    }

    public void setMessagingServiceHost(String messagingServiceHost) {
        this.messagingServiceHost = messagingServiceHost;
    }

    public int getMessagingServicePort() {
        return messagingServicePort;
    }

    public void setMessagingServicePort(int messagingServicePort) {
        this.messagingServicePort = messagingServicePort;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getCertFile() {
        return certFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
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

    public static MqttGatewayOptions fromEnv(Map<String, String> env) {

        MqttGatewayOptions options = new MqttGatewayOptions();

        options.setMessagingServiceHost(getEnv(env, "MESSAGING_SERVICE_HOST")
                .orElse("0.0.0.0"));

        options.setMessagingServicePort(getEnv(env, "MESSAGING_SERVICE_PORT")
                .map(Integer::parseInt)
                .orElse(5672));

        options.setStartupTimeout(getEnv(env, "ENMASSE_MQTT_STARTUPTIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(20)));

        options.setBindAddress(getEnv(env, "ENMASSE_MQTT_BINDADDRESS")
                .orElse("0.0.0.0"));

        options.setListenPort(getEnv(env, "ENMASSE_MQTT_LISTENPORT")
                .map(Integer::parseInt)
                .orElse(1883));

        options.setSsl(getEnv(env, "ENMASSE_MQTT_SSL")
                .map(Boolean::parseBoolean)
                .orElse(false));

        options.setMaxMessageSize(getEnv(env, "ENMASSE_MQTT_MAXMESSAGESIZE")
                .map(Integer::parseInt)
                .orElse(131072));

        options.setCertFile(getEnv(env, "ENMASSE_MQTT_CERTFILE")
                .orElse("./src/test/resources/tls/server-cert.pem"));

        options.setKeyFile(getEnv(env, "ENMASSE_MQTT_KEYFILE")
                .orElse("./src/test/resources/tls/server-key.pem"));

        return options;
    }

    @Override
    public String toString() {
        return "MqttGatewayOptions{" +
                "bindAddress='" + bindAddress + '\'' +
                ", listenPort=" + listenPort +
                ", maxMessageSize=" + maxMessageSize +
                ", messagingServiceHost='" + messagingServiceHost + '\'' +
                ", messagingServicePort=" + messagingServicePort +
                ", ssl=" + ssl +
                ", certFile='" + certFile + '\'' +
                ", keyFile='" + keyFile + '\'' +
                ", startupTimeout=" + startupTimeout +
                '}';
    }
}
