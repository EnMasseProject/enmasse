/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import java.net.HttpURLConnection;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hono.util.TenantConstants;
import org.eclipse.hono.util.TenantResult;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.v1.IoTTenant;
import io.enmasse.iot.model.v1.IoTTenantBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopTracerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AbstractTenantServiceTest {

    private static final ObjectMeta DEFAULT_METADATA = new ObjectMetaBuilder()
            .withNamespace("foo")
            .withName("bar")
            .build();

    private final AbstractTenantService service;

    public AbstractTenantServiceTest() {
        this.service = new AbstractTenantService() {};
        this.service.tracer = NoopTracerFactory.create();
        this.service.configuration = new TenantServiceConfigProperties();
    }

    @Test
    public void testConvertForHonoNoStatus() {
        final IoTTenant project = new IoTTenantBuilder()
                .withMetadata(DEFAULT_METADATA)
                .build();
        var result = service.convertToHono(project, NoopSpan.INSTANCE.context());

        assertNotFound(result);
    }

    @Test
    public void testConvertForHonoNoAcceptedStatus() {
        final IoTTenant project = new IoTTenantBuilder()
                .withMetadata(DEFAULT_METADATA)
                .withNewStatus()
                .endStatus()
                .build();

        var result = service.convertToHono(project, NoopSpan.INSTANCE.context());

        assertNotFound(result);
    }

    @Test
    public void testConvertForHonoNoAcceptedConfiguration() {
        final IoTTenant project = new IoTTenantBuilder()
                .withMetadata(DEFAULT_METADATA)
                .withNewStatus()
                .withNewAccepted()
                .endAccepted()
                .endStatus()
                .build();

        var result = service.convertToHono(project, NoopSpan.INSTANCE.context());

        assertNotFound(result);
    }

    @Test
    public void testConvertForHonoEmptyAcceptedConfiguration() {
        final IoTTenant project = new IoTTenantBuilder()
                .withMetadata(DEFAULT_METADATA)
                .withNewStatus()
                .withNewAccepted()
                .withConfiguration(new HashMap<>())
                .endAccepted()
                .endStatus()
                .build();

        var result = service.convertToHono(project, NoopSpan.INSTANCE.context());

        assertFound(result);
    }

    private void assertNotFound(TenantResult<JsonObject> result) {
        assertNotNull(result);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, result.getStatus());
    }

    private void assertFound(TenantResult<JsonObject> result) {
        assertNotNull(result);
        assertEquals(HttpURLConnection.HTTP_OK, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotAfter() throws Exception {

        var project = new ObjectMapper().readValue(AbstractTenantServiceTest.class.getResourceAsStream("/test-with-cert.json"), IoTTenant.class);

        var input = (List<?>) ((Map<String, Object>) project.getStatus().getAccepted().getConfiguration()).get(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA);

        assertNotNull(input);
        assertThat(input, iterableWithSize(1));

        var result = AbstractTenantService.stripInvalidTrustAnchors(
                OffsetDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                new JsonArray(input),
                NoopSpan.INSTANCE);

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotBefore() throws Exception {

        var project = new ObjectMapper().readValue(AbstractTenantServiceTest.class.getResourceAsStream("/test-with-cert.json"), IoTTenant.class);

        var input = (List<?>) ((Map<String, Object>) project.getStatus().getAccepted().getConfiguration()).get(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA);

        assertNotNull(input);
        assertThat(input, iterableWithSize(1));

        var result = AbstractTenantService.stripInvalidTrustAnchors(
                OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                new JsonArray(input),
                NoopSpan.INSTANCE);

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBetweenBeforeAndAfter() throws Exception {

        var project = new ObjectMapper().readValue(AbstractTenantServiceTest.class.getResourceAsStream("/test-with-cert.json"), IoTTenant.class);

        var input = (List<?>) ((Map<String, Object>) project.getStatus().getAccepted().getConfiguration()).get(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA);

        assertNotNull(input);
        assertThat(input, iterableWithSize(1));

        var result = AbstractTenantService.stripInvalidTrustAnchors(
                OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                new JsonArray(input),
                NoopSpan.INSTANCE);

        assertNotNull(result);
        assertThat(result, iterableWithSize(1));
    }
}
