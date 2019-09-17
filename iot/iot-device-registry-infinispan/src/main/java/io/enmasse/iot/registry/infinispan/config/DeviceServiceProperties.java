/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.enmasse.iot.service.base.infinispan.config.InfinispanProperties;

@Configuration
@ConfigurationProperties(InfinispanProperties.CONFIG_BASE + ".registry.device")
public class DeviceServiceProperties {

    private static final int DEFAULT_TASK_EXECUTOR_QUEUE_SIZE = 1024;
    private static final Duration DEFAULT_CREDENTIALS_TTL = Duration.ofMinutes(1);

    private int taskExecutorQueueSize = DEFAULT_TASK_EXECUTOR_QUEUE_SIZE;
    private Duration credentialsTtl = DEFAULT_CREDENTIALS_TTL;

    public int getTaskExecutorQueueSize() {
        return this.taskExecutorQueueSize;
    }

    public void setTaskExecutorQueueSize(final int taskExecutorQueueSize) {
        this.taskExecutorQueueSize = taskExecutorQueueSize;
    }

    public Duration getCredentialsTtl() {
        return credentialsTtl;
    }

    public void setCredentialsTtl(final Duration credentialsTtl) {
        if ( credentialsTtl.toSeconds() <= 0 ) {
            throw new IllegalArgumentException("'credentialsTtl' must be a positive duration of at least one second");
        }
        this.credentialsTtl = credentialsTtl;
    }

}
