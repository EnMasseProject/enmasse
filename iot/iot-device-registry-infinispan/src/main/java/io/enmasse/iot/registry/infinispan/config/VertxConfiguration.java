/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import org.eclipse.hono.config.VertxProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.infinispan.config.InfinispanProperties;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

@Configuration
public class VertxConfiguration {

    /**
     * Exposes a Vert.x instance as a Spring bean.
     * <p>
     * This method creates new Vert.x default options and invokes
     * {@link VertxProperties#configureVertx(VertxOptions)} on the object returned
     * by {@link #vertxProperties()}.
     *
     * @return The Vert.x instance.
     */
    @Bean
    public Vertx vertx() {
        return Vertx.vertx(vertxProperties().configureVertx(new VertxOptions()));
    }

    /**
     * Exposes configuration properties for Vert.x.
     *
     * @return The properties.
     */
    @ConfigurationProperties(InfinispanProperties.CONFIG_BASE + ".vertx")
    @Bean
    public VertxProperties vertxProperties() {
        return new VertxProperties();
    }
}
