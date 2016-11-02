/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.mqtt;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * EnMasse MQTT FrontEnd main application class
 */
@SpringBootApplication // same as using @Configuration, @EnableAutoConfiguration and @ComponentScan
public class Application {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Vertx vertx = Vertx.vertx();

    @Value(value = "${enmasse.mqtt.maxinstances:1}")
    private int maxInstances;
    @Autowired
    private MqttFrontEnd mqttFrontEnd;

    @PostConstruct
    public void registerVerticles() {

        deployVerticles(this.maxInstances);
    }

    private void deployVerticles(int instanceCount) {

        LOG.debug("Starting up {} instances of MQTT front end verticle", instanceCount);

        for (int i = 1; i <= instanceCount; i++) {
            final int instanceId = i;
            this.vertx.deployVerticle(this.mqttFrontEnd, done -> {
                if (done.succeeded()) {
                    LOG.debug("Verticle instance {} deployed [{}]", instanceCount, done.result());
                } else {
                    LOG.debug("Failed to deploy verticle instance {}", instanceId, done.cause());
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {

    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
