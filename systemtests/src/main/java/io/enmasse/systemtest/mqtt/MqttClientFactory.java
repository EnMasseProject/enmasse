/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.mqtt;

import io.enmasse.systemtest.*;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MqttClientFactory {

    private final Kubernetes kubernetes;
    private final Environment environment;
    private final AddressSpace defaultAddressSpace;
    private final String username;
    private final String password;
    private final List<MqttClient> clients = new ArrayList<>();

    public MqttClientFactory(Kubernetes kubernetes, Environment environment, AddressSpace defaultAddressSpace, String username, String password) {
        this.kubernetes = kubernetes;
        this.environment = environment;
        this.defaultAddressSpace = defaultAddressSpace;
        this.username = username;
        this.password = password;
    }

    public static SSLContext tryGetSSLContext(final String... protocols) throws NoSuchAlgorithmException {
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

    public void close() throws Exception {

        for (MqttClient client : this.clients) {
            client.close();
        }
        this.clients.clear();
    }

    public MqttClient createClient() throws Exception {
        assertNotNull(defaultAddressSpace, "Address space is null");
        return createClient(defaultAddressSpace);
    }

    public MqttClient createClient(AddressSpace addressSpace) throws Exception {

        MqttConnectOptions options = new MqttConnectOptions();
        Endpoint mqttEndpoint;

        if (environment.useTLS()) {
            mqttEndpoint = addressSpace.getEndpoint("mqtt");
            if (mqttEndpoint == null) {
                String externalEndpointName = TestUtils.getExternalEndpointName(addressSpace, "mqtt");
                mqttEndpoint = kubernetes.getExternalEndpoint(addressSpace.getNamespace(), externalEndpointName);
            }
            SSLContext sslContext = tryGetSSLContext("TLSv1.2", "TLSv1.1", "TLS", "TLSv1");
            sslContext.init(null, new X509TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            SSLSocketFactory sslSocketFactory = new SNISettingSSLSocketFactory(sslContext.getSocketFactory(), mqttEndpoint.getHost());

            options.setSocketFactory(sslSocketFactory);

            if (!TestUtils.resolvable(mqttEndpoint)) {
                mqttEndpoint = new Endpoint("localhost", 443);
            }

        } else {
            mqttEndpoint = this.kubernetes.getEndpoint(addressSpace.getNamespace(), "mqtt", "mqtt");
        }

        if (username != null && password != null) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }

        MqttClient client = new MqttClient(mqttEndpoint, options);
        this.clients.add(client);
        return client;
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
        public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose) throws IOException {
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
}
