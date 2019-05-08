/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.user.model.v1.User;

public class IoTProjectTestContext {

    private String namespace;
    private IoTProject project;

    private String deviceId;
    private String deviceAuthId;
    private String devicePassword;
    private HttpAdapterClient httpAdapterClient;

    private User amqpUser;
    private AmqpClient amqpClient;

    public IoTProjectTestContext(String namespace, IoTProject project) {
        this.namespace = namespace;
        this.project = project;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceAuthId() {
        return deviceAuthId;
    }

    public void setDeviceAuthId(String deviceAuthId) {
        this.deviceAuthId = deviceAuthId;
    }

    public String getDevicePassword() {
        return devicePassword;
    }

    public void setDevicePassword(String devicePassword) {
        this.devicePassword = devicePassword;
    }

    public HttpAdapterClient getHttpAdapterClient() {
        return httpAdapterClient;
    }

    public void setHttpAdapterClient(HttpAdapterClient httpAdapterClient) {
        this.httpAdapterClient = httpAdapterClient;
    }

    public User getAmqpUser() {
        return amqpUser;
    }

    public void setAmqpUser(User amqpUser) {
        this.amqpUser = amqpUser;
    }

    public AmqpClient getAmqpClient() {
        return amqpClient;
    }

    public void setAmqpClient(AmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }

    public String getNamespace() {
        return namespace;
    }

    public IoTProject getProject() {
        return project;
    }

}
