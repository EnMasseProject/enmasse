/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.jdbc.config;

import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_ADAPTER;
import static io.enmasse.iot.registry.jdbc.Profiles.PROFILE_REGISTRY_MANAGEMENT;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.enmasse.iot.utils.ConfigBase;

@Configuration
@ConfigurationProperties(ConfigBase.CONFIG_BASE + ".registry.device")
@Profile({PROFILE_REGISTRY_ADAPTER, PROFILE_REGISTRY_MANAGEMENT})
public class DeviceServiceProperties {

    private static final int DEFAULT_TASK_EXECUTOR_QUEUE_SIZE = 1024;
    private static final Duration DEFAULT_CREDENTIALS_TTL = Duration.ofMinutes(1);
    private static final Duration DEFAULT_REGISTRATION_TTL = Duration.ofMinutes(1);
    private static final int DEFAULT_MAX_BCRYPT_ITERATIONS = 10;

    private int taskExecutorQueueSize = DEFAULT_TASK_EXECUTOR_QUEUE_SIZE;

    private Duration credentialsTtl = DEFAULT_CREDENTIALS_TTL;
    private Duration registrationTtl = DEFAULT_REGISTRATION_TTL;

    private int maxBcryptIterations = DEFAULT_MAX_BCRYPT_ITERATIONS;

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
        if (credentialsTtl.toSeconds() <= 0) {
            throw new IllegalArgumentException("'credentialsTtl' must be a positive duration of at least one second");
        }
        this.credentialsTtl = credentialsTtl;
    }

    public Duration getRegistrationTtl() {
        return registrationTtl;
    }

    public void setRegistrationTtl(final Duration registrationTtl) {
        if (registrationTtl.toSeconds() <= 0) {
            throw new IllegalArgumentException("'registrationTtl' must be a positive duration of at least one second");
        }
        this.registrationTtl = registrationTtl;
    }

    /**
     * Gets the maximum number of iterations to use for bcrypt
     * password hashes.
     * <p>
     * The default value of this property is 10.
     *
     * @return The maximum number.
     */
    public int getMaxBcryptIterations() {
        return maxBcryptIterations;
    }

    /**
     * Sets the maximum number of iterations to use for bcrypt
     * password hashes.
     * <p>
     * The default value of this property is 10.
     *
     * @param iterations The maximum number.
     * @throws IllegalArgumentException if iterations is &lt; 4 or &gt; 31.
     */
    public void setMaxBcryptIterations(final int iterations) {
        if (iterations < 4 || iterations > 31) {
            throw new IllegalArgumentException("iterations must be > 3 and < 32");
        } else {
            maxBcryptIterations = iterations;
        }
    }
}
