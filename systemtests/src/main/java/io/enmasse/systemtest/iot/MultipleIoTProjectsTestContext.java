/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.user.model.v1.User;

public class MultipleIoTProjectsTestContext {
    private IoTProject project;
    private HttpAdapterClient httpAdapterClient;
    private AmqpClient amqpClient;
    private User amqpUser;

    public MultipleIoTProjectsTestContext(IoTProject project, HttpAdapterClient httpAdapterClient, AmqpClient amqpClient, User amqpUser) {
        this.project = project;
        this.httpAdapterClient = httpAdapterClient;
        this.amqpClient = amqpClient;
        this.amqpUser = amqpUser;
    }

    public IoTProject getProject() {
        return project;
    }

    public HttpAdapterClient getHttpAdapterClient() {
        return httpAdapterClient;
    }

    public AmqpClient getAmqpClient() {
        return amqpClient;
    }

    public User getAmqpUser() {
        return amqpUser;
    }

}
