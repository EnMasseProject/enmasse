/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.mqtt;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiFunction;

public class MqttClientFactory {

    private static final Logger LOGGER = CustomLogger.getLogger();

    private final String SERVER_URI_TEMPLATE = "tcp://%s:%s";
    private final String TLS_SERVER_URI_TEMPLATE = "ssl://%s:%s";

    private final Set<AutoCloseable> connectedClients = new CopyOnWriteArraySet<>(); // using a copy-on-write set allows to mutate the set while traversing it

    private final AddressSpace defaultAddressSpace;
    private final String username;
    private final String password;

    public MqttClientFactory(AddressSpace defaultAddressSpace, UserCredentials credentials) {
        this.defaultAddressSpace = defaultAddressSpace;
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
    }

    public SSLContext tryGetSSLContext(final String... protocols) throws NoSuchAlgorithmException {
        for (String protocol : protocols) {
            try {
                return SSLContext.getInstance(protocol);
            } catch (NoSuchAlgorithmException e) {
                // pass and try the next protocol in the list
            }
        }
        throw new NoSuchAlgorithmException(String.format("Could not create SSLContext with one of the requested protocols: %s",
                Arrays.toString(protocols)));
    }

    public Builder build() {
        return new Builder() {
            Endpoint endpoint;
            AddressSpace addressSpace = defaultAddressSpace;
            MqttConnectOptions mqttConnectOptions;
            String clientId = UUID.randomUUID().toString();

            @Override
            public Builder endpoint(Endpoint endpoint) {
                this.endpoint = endpoint;
                return this;
            }

            @Override
            public Builder addressSpace(AddressSpace addressSpace) {
                this.addressSpace = addressSpace;
                return this;
            }

            @Override
            public Builder mqttConnectionOptions(MqttConnectOptions mqttConnectOptions) {
                this.mqttConnectOptions = mqttConnectOptions;
                return this;
            }

            @Override
            public Builder clientId(String clientId) {
                this.clientId = clientId;
                return this;
            }

            @Override
            public IMqttClient create() throws Exception {
                if (mqttConnectOptions == null) {
                    mqttConnectOptions = new MqttConnectOptions();
                    mqttConnectOptions.setAutomaticReconnect(true);
                    mqttConnectOptions.setHttpsHostnameVerificationEnabled(false);
                }
                return MqttClientFactory.this.create(MqttClient::new, DelegatingMqttClient::new,
                        endpoint, addressSpace, mqttConnectOptions, clientId);
            }

            @Override
            public IMqttAsyncClient createAsync() throws Exception {
                return MqttClientFactory.this.create(MqttAsyncClient::new, DelegatingMqttAsyncClient::new,
                        endpoint, addressSpace, mqttConnectOptions, clientId);
            }
        };
    }

    public IMqttClient create() throws Exception {
        return build().create();
    }

    private <C> C create(ConnectionFactory<? extends C> factory, BiFunction<C, MqttConnectOptions, C> delegator,
                         Endpoint endpoint, AddressSpace addressSpace, MqttConnectOptions options,
                         String clientId) throws Exception {

        Endpoint mqttEndpoint;

        if (endpoint == null) {
            mqttEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "mqtt");
            if (mqttEndpoint == null) {
                String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "mqtt");
                mqttEndpoint = Kubernetes.getInstance().getExternalEndpoint(externalEndpointName + "-" +
                        AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
            }
        } else {
            mqttEndpoint = endpoint;
        }

        if (options.getSocketFactory() == null) {
            SSLContext sslContext = tryGetSSLContext("TLSv1.2", "TLSv1.1", "TLS", "TLSv1");
            sslContext.init(null, new X509TrustManager[]{new MyX509TrustManager()}, new SecureRandom());

            SSLSocketFactory sslSocketFactory = new SNISettingSSLSocketFactory(sslContext.getSocketFactory(), mqttEndpoint.getHost());

            options.setSocketFactory(sslSocketFactory);
        } else if (options.getSocketFactory() instanceof SSLSocketFactory) {
            options.setSocketFactory(new SNISettingSSLSocketFactory((SSLSocketFactory) options.getSocketFactory(), mqttEndpoint.getHost()));
        }

        if (!TestUtils.resolvable(mqttEndpoint)) {
            mqttEndpoint = new Endpoint("localhost", 443);
        }

        LOGGER.info("Using mqtt endpoint {}", mqttEndpoint);

        if (username != null && password != null) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        final String uriFormat = options.getSocketFactory() instanceof SSLSocketFactory
                ? TLS_SERVER_URI_TEMPLATE
                : SERVER_URI_TEMPLATE;
        String serverURI = String.format(uriFormat, mqttEndpoint.getHost(), mqttEndpoint.getPort());
        return delegator.apply(factory.newInstance(serverURI, clientId, new MemoryPersistence()), options);
    }

    public void close() {
        for (AutoCloseable client : connectedClients) {
            try {
                client.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
        connectedClients.clear();
    }

    @FunctionalInterface
    private interface ConnectionFactory<C> {
        C newInstance(String serverUri, String clientId, MqttClientPersistence persistence) throws MqttException;
    }


    public interface Builder {

        Builder endpoint(Endpoint endpoint);

        Builder addressSpace(AddressSpace addressSpace);

        Builder mqttConnectionOptions(MqttConnectOptions options);

        Builder clientId(String clientId);

        IMqttClient create() throws Exception;

        IMqttAsyncClient createAsync() throws Exception;
    }

    private static class SNISettingSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory socketFactory;

        private final List<SNIServerName> sniHostNames;

        SNISettingSSLSocketFactory(final SSLSocketFactory socketFactory,
                                   final String host) {
            this.socketFactory = socketFactory;
            this.sniHostNames = Collections.singletonList(new SNIHostName(host));
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return socketFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return socketFactory.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(final Socket socket, final String host,
                                   final int port, final boolean autoClose) throws IOException {
            return setHostnameParameter(socketFactory.createSocket(socket, host, port, autoClose));
        }

        private Socket setHostnameParameter(final Socket newSocket) {
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setServerNames(this.sniHostNames);
            ((SSLSocket) newSocket).setSSLParameters(sslParameters);
            return newSocket;
        }

        @Override
        public Socket createSocket(final Socket socket, final InputStream inputStream, final boolean b)
                throws IOException {
            return setHostnameParameter(socketFactory.createSocket(socket, inputStream, b));
        }

        @Override
        public Socket createSocket() throws IOException {
            return setHostnameParameter(socketFactory.createSocket());
        }

        @Override
        public Socket createSocket(final String s, final int i) throws IOException {
            return setHostnameParameter(socketFactory.createSocket(s, i));
        }

        @Override
        public Socket createSocket(final String s, final int i, final InetAddress inetAddress, final int i1)
                throws IOException {
            return setHostnameParameter(socketFactory.createSocket(s, i, inetAddress, i1));
        }

        @Override
        public Socket createSocket(final InetAddress inetAddress, final int i) throws IOException {
            return setHostnameParameter(socketFactory.createSocket(inetAddress, i));
        }

        @Override
        public Socket createSocket(final InetAddress inetAddress,
                                   final int i,
                                   final InetAddress inetAddress1,
                                   final int i1) throws IOException {
            return setHostnameParameter(socketFactory.createSocket(inetAddress, i, inetAddress1, i1));
        }
    }

    private static class MyX509TrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private class DelegatingMqttAsyncClient implements IMqttAsyncClient, AutoCloseable {

        private IMqttAsyncClient mqttClient;
        private MqttConnectOptions options;

        DelegatingMqttAsyncClient(IMqttAsyncClient mqttClient, MqttConnectOptions options) {
            this.mqttClient = mqttClient;
            this.options = options;
            connectedClients.add(this);
        }

        @Override
        public void close() throws MqttException {
            connectedClients.remove(this);
            mqttClient.close();
        }

        public IMqttToken connect() throws MqttException {
            return mqttClient.connect(options);
        }

        public IMqttToken connect(MqttConnectOptions options) {
            throw new UnsupportedOperationException("Use the zero args this method.");
        }

        public IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException {
            return mqttClient.connect(options, userContext, callback);
        }

        public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback) {
            throw new UnsupportedOperationException("Use the zero args this method.");
        }

        public IMqttToken disconnect() throws MqttException {
            return mqttClient.disconnect();
        }

        public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
            return mqttClient.disconnect(quiesceTimeout);
        }

        public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException {
            return mqttClient.disconnect(userContext, callback);
        }

        public IMqttToken disconnect(long quiesceTimeout, Object userContext,
                                                        IMqttActionListener callback) throws MqttException {
            return mqttClient.disconnect(quiesceTimeout, userContext, callback);
        }

        public void disconnectForcibly() throws MqttException {
            mqttClient.disconnectForcibly();
        }

        public void disconnectForcibly(long disconnectTimeout) throws MqttException {
            mqttClient.disconnectForcibly(disconnectTimeout);
        }

        public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
            mqttClient.disconnectForcibly(quiesceTimeout, disconnectTimeout);
        }

        public boolean isConnected() {
            return mqttClient.isConnected();
        }

        public String getClientId() {
            return mqttClient.getClientId();
        }

        public String getServerURI() {
            return mqttClient.getServerURI();
        }

        public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException {
            return mqttClient.publish(topic, payload, qos, retained);
        }

        public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained,
                                          Object userContext, IMqttActionListener callback) throws MqttException {
            return mqttClient.publish(topic, payload, qos, retained, userContext, callback);
        }

        public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException {
            return mqttClient.publish(topic, message);
        }

        public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext,
                                          IMqttActionListener callback) throws MqttException {
            return mqttClient.publish(topic, message, userContext, callback);
        }

        public void reconnect() throws MqttException {
            mqttClient.reconnect();
        }

        public boolean removeMessage(IMqttDeliveryToken token) throws MqttException {
            return mqttClient.removeMessage(token);
        }

        public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
            return mqttClient.subscribe(topicFilter, qos);
        }

        public IMqttToken subscribe(String topicFilter, int qos, Object userContext,
                                    IMqttActionListener callback) throws MqttException {
            return mqttClient.subscribe(topicFilter, qos, userContext, callback);
        }

        public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException {
            return mqttClient.subscribe(topicFilters, qos);
        }

        public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext,
                                    IMqttActionListener callback) throws MqttException {
            return mqttClient.subscribe(topicFilters, qos, userContext, callback);
        }

        public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback,
                                    IMqttMessageListener messageListener) throws MqttException {
            return mqttClient.subscribe(topicFilter, qos, userContext, callback, messageListener);
        }

        public IMqttToken subscribe(String topicFilter, int qos,
                                    IMqttMessageListener messageListener) throws MqttException {
            return mqttClient.subscribe(topicFilter, qos, messageListener);
        }

        public IMqttToken subscribe(String[] topicFilters, int[] qos,
                                    IMqttMessageListener[] messageListeners) throws MqttException {
            return mqttClient.subscribe(topicFilters, qos, messageListeners);
        }

        public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext,
                                    IMqttActionListener callback, IMqttMessageListener[] messageListeners)
                throws MqttException {
            return mqttClient.subscribe(topicFilters, qos, userContext, callback, messageListeners);
        }

        public IMqttToken unsubscribe(String topicFilter) throws MqttException {
            return mqttClient.unsubscribe(topicFilter);
        }

        public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
            return mqttClient.unsubscribe(topicFilters);
        }

        public IMqttToken unsubscribe(String topicFilter, Object userContext, IMqttActionListener callback) throws MqttException {
            return mqttClient.unsubscribe(topicFilter, userContext, callback);
        }

        public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback) throws MqttException {
            return mqttClient.unsubscribe(topicFilters, userContext, callback);
        }

        public void setCallback(MqttCallback callback) {
            mqttClient.setCallback(callback);
        }

        public IMqttDeliveryToken[] getPendingDeliveryTokens() {
            return mqttClient.getPendingDeliveryTokens();
        }

        public void setManualAcks(boolean manualAcks) {
            mqttClient.setManualAcks(manualAcks);
        }

        public void messageArrivedComplete(int messageId, int qos) throws MqttException {
            mqttClient.messageArrivedComplete(messageId, qos);
        }

    }

    private class DelegatingMqttClient implements IMqttClient, AutoCloseable {
        private final IMqttClient mqttClient;
        private final MqttConnectOptions options;

        DelegatingMqttClient(IMqttClient mqttClient, MqttConnectOptions options) {
            this.mqttClient = mqttClient;
            this.options = options;
            connectedClients.add(this);
        }

        @Override
        public void connect(MqttConnectOptions options) {
            throw new UnsupportedOperationException("Use the zero args  this method.");
        }

        @Override
        public IMqttToken connectWithResult(MqttConnectOptions options) {
            throw new UnsupportedOperationException("Use the zero args this method.");
        }

        @Override
        public void connect() throws MqttException {
            this.mqttClient.connect(options);
        }

        @Override
        public void disconnect() throws MqttException {
            this.mqttClient.disconnect();
        }

        @Override
        public void disconnect(long quiesceTimeout) throws MqttException {
            this.mqttClient.disconnect(quiesceTimeout);
        }

        @Override
        public void disconnectForcibly() throws MqttException {
            this.mqttClient.disconnectForcibly();
        }

        @Override
        public void disconnectForcibly(long disconnectTimeout) throws MqttException {
            this.mqttClient.disconnectForcibly(disconnectTimeout);
        }

        @Override
        public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
            this.mqttClient.disconnectForcibly(quiesceTimeout, disconnectTimeout);
        }

        @Override
        public void reconnect() throws MqttException {
            this.mqttClient.reconnect();
        }

        @Override
        public void subscribe(String topicFilter) throws MqttException {
            this.mqttClient.subscribe(topicFilter);
        }

        @Override
        public void subscribe(String[] topicFilters) throws MqttException {
            this.mqttClient.subscribe(topicFilters);
        }

        @Override
        public void subscribe(String topicFilter, int qos) throws MqttException {
            this.mqttClient.subscribe(topicFilter, qos);
        }

        @Override
        public void subscribe(String[] topicFilters, int[] qos) throws MqttException {
            this.mqttClient.subscribe(topicFilters, qos);
        }

        @Override
        public void subscribe(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
            this.mqttClient.subscribe(topicFilter, messageListener);
        }

        @Override
        public void subscribe(String[] topicFilters, IMqttMessageListener[] messageListeners) throws MqttException {
            this.mqttClient.subscribe(topicFilters, messageListeners);
        }

        @Override
        public void subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException {
            this.mqttClient.subscribe(topicFilter, qos, messageListener);
        }

        @Override
        public void subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException {
            this.mqttClient.subscribe(topicFilters, qos, messageListeners);
        }

        @Override
        public IMqttToken subscribeWithResponse(String topicFilter) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilter);
        }

        @Override
        public IMqttToken subscribeWithResponse(String topicFilter, IMqttMessageListener messageListener) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilter, messageListener);
        }

        @Override
        public IMqttToken subscribeWithResponse(String topicFilter, int qos) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilter, qos);
        }

        @Override
        public IMqttToken subscribeWithResponse(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilter, qos, messageListener);
        }

        @Override
        public IMqttToken subscribeWithResponse(String[] topicFilters) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilters);
        }

        @Override
        public IMqttToken subscribeWithResponse(String[] topicFilters, IMqttMessageListener[] messageListeners) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilters, messageListeners);
        }

        @Override
        public IMqttToken subscribeWithResponse(String[] topicFilters, int[] qos) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilters, qos);
        }

        @Override
        public IMqttToken subscribeWithResponse(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException {
            return this.mqttClient.subscribeWithResponse(topicFilters, qos, messageListeners);
        }

        @Override
        public void unsubscribe(String topicFilter) throws MqttException {
            this.mqttClient.unsubscribe(topicFilter);
        }

        @Override
        public void unsubscribe(String[] topicFilters) throws MqttException {
            this.mqttClient.unsubscribe(topicFilters);
        }

        @Override
        public void publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException {
            this.mqttClient.publish(topic, payload, qos, retained);
        }

        @Override
        public void publish(String topic, MqttMessage message) throws MqttException {
            this.mqttClient.publish(topic, message);
        }

        @Override
        public void setCallback(MqttCallback callback) {
            this.mqttClient.setCallback(callback);
        }

        @Override
        public MqttTopic getTopic(String topic) {
            return this.mqttClient.getTopic(topic);
        }

        @Override
        public boolean isConnected() {
            return this.mqttClient.isConnected();
        }

        @Override
        public String getClientId() {
            return this.mqttClient.getClientId();
        }

        @Override
        public String getServerURI() {
            return this.mqttClient.getServerURI();
        }

        @Override
        public IMqttDeliveryToken[] getPendingDeliveryTokens() {
            return this.mqttClient.getPendingDeliveryTokens();
        }

        @Override
        public void setManualAcks(boolean manualAcks) {
            this.mqttClient.setManualAcks(manualAcks);
        }

        @Override
        public void messageArrivedComplete(int messageId, int qos) throws MqttException {
            this.mqttClient.messageArrivedComplete(messageId, qos);
        }

        @Override
        public void close() throws MqttException {
            connectedClients.remove(this);
            this.mqttClient.close();
        }
    }
}
