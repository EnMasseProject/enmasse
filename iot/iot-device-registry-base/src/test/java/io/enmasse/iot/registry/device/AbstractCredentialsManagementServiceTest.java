/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.device;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.registry.device.DeviceKey;
import io.enmasse.iot.registry.device.AbstractCredentialsManagementService;
import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;

/**
 * Tests for {@link AbstractCredentialsManagementService}.
 *
 */
public class AbstractCredentialsManagementServiceTest {

    private AbstractCredentialsManagementService service;

    @BeforeEach
    public void setup() {
        this.service = new AbstractCredentialsManagementService(null, 1) {

            @Override
            protected CompletableFuture<OperationResult<Void>> processSet(DeviceKey key, Optional<String> resourceVersion, List<CommonCredential> credentials, Span span) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            protected CompletableFuture<OperationResult<List<CommonCredential>>> processGet(DeviceKey key, Span span) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @AfterEach
    public void cleanup() {
        this.service.close();
    }

    /**
     * Test if the abstract implementation detects the invalid credential information.
     * <br>
     * This test checks if the {@link AbstractCredentialsManagementService} detects, and rejects an
     * invalid credential set. For that the actual implementation of the service can simply return
     * {@code null}.
     *
     * @throws Exception if something goes wrong. It should not.
     */
    @Test
    public void testValidationFailure() throws Exception {
        final PasswordCredential invalidCredentials = new PasswordCredential();
        final List<CommonCredential> credentials = Collections.singletonList(invalidCredentials);

        final CompletableFuture<OperationResult<Void>> f = this.service.processSet("foo", "bar", Optional.empty(), credentials, NoopSpan.INSTANCE);
        final OperationResult<Void> r = f.get();
        assertThat(r, notNullValue());
        assertThat(r.isError(), Is.is(true));
        assertThat(r.getStatus(), Is.is(HttpURLConnection.HTTP_BAD_REQUEST));
    }

}
