/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.impl;

import static io.opentracing.tag.Tags.HTTP_STATUS;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.eclipse.hono.service.tenant.TenantService;
import org.eclipse.hono.tracing.TracingHelper;
import org.eclipse.hono.util.CacheDirective;
import org.eclipse.hono.util.TenantConstants;
import org.eclipse.hono.util.TenantResult;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import io.enmasse.iot.model.v1.IoTTenant;
import io.enmasse.iot.service.base.AbstractTenantBasedService;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class AbstractTenantService extends AbstractTenantBasedService implements TenantService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractTenantService.class);

    protected static final TenantResult<JsonObject> RESULT_NOT_FOUND = TenantResult.from(HTTP_NOT_FOUND);

    protected TenantServiceConfigProperties configuration;

    protected Tracer tracer;

    @Autowired
    public void setConfig(final TenantServiceConfigProperties configuration) {
        this.configuration = configuration;
    }

    @Autowired
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Future<TenantResult<JsonObject>> get(final String tenantId) {
        return get(tenantId, NoopSpan.INSTANCE);
    }

    @Override
    public Future<TenantResult<JsonObject>> get(final X500Principal subjectDn) {
        return get(subjectDn, NoopSpan.INSTANCE);
    }

    protected TenantResult<JsonObject> convertToHono(final IoTTenant project, final SpanContext spanContext) {

        final String tenantName = String.format("%s.%s", project.getMetadata().getNamespace(), project.getMetadata().getName());

        final Span span = TracingHelper
                .buildChildSpan(this.tracer, spanContext, "convertToHono", getClass().getSimpleName())
                .withTag(TracingHelper.TAG_TENANT_ID, tenantName)
                .start();

        try {

            if (project.getStatus() == null || project.getStatus().getAccepted() == null) {
                // controller has not yet processed the configuration ... handle as "not found"
                log.info("IoTTenant is missing '.status.accepted' section");
                TracingHelper.logError(span, "IoTTenant is missing '.status.accepted' section");
                HTTP_STATUS.set(span, RESULT_NOT_FOUND.getStatus());
                return RESULT_NOT_FOUND;
            }

            if (project.getStatus().getAccepted().getConfiguration() == null) {
                // controller processed the configuration, but rejected it ... handle as "not found"
                log.info("IoTTenant rejected the tenant configuration");
                TracingHelper.logError(span, "IoTTenant rejected the tenant configuration");
                HTTP_STATUS.set(span, RESULT_NOT_FOUND.getStatus());
                return RESULT_NOT_FOUND;
            }

            // get configuration as JsonObject

            final JsonObject payload = JsonObject.mapFrom(project.getStatus().getAccepted().getConfiguration());

            // remove invalid trust anchors, as this involves the current time, we cannot
            // prepare this, and only do it on a "per-request" basis.

            payload.put(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA,
                    stripInvalidTrustAnchors(
                            Instant.now(),
                            payload.getJsonArray(TenantConstants.FIELD_PAYLOAD_TRUSTED_CA),
                            span));

            // always add (and override) the tenant id

            payload.put(TenantConstants.FIELD_PAYLOAD_TENANT_ID, tenantName);

            // return result

            HTTP_STATUS.set(span, HttpURLConnection.HTTP_OK);
            return TenantResult.from(
                    HttpURLConnection.HTTP_OK,
                    payload,
                    CacheDirective.maxAgeDirective(this.configuration.getCacheTimeToLive().getSeconds()));

        } finally {
            // close span
            span.finish();
        }
    }

    /**
     * Check if a trust anchor is valid.
     * <p>
     * A trust anchor is considered valid if all of the following conditions are valid:
     * <ul>
     * <li>The "not before" and "not after" values are non null, non empty, and valid timestamps</li>
     * <li>The provided "now" instant is between "not before" and "not after"</li>
     * <li>If the trust anchor is "enabled"</li>
     * </ul>
     *
     * @param now The instant to consider as "now".
     * @param jsonArray The JSON array to all available trust anchors.
     * @param span Tracing span to log to.
     * @return The array of only valid trust anchors, or {@code null} if there are none.
     */
    protected static JsonArray stripInvalidTrustAnchors(final Instant now, final JsonArray jsonArray, final Span span) {

        // quick check for "null", return early

        if (jsonArray == null) {
            return null;
        }

        // process

        JsonArray result = null;

        for (final Object item : jsonArray) {

            // drop every non-object

            if (!(item instanceof JsonObject)) {
                continue;
            }

            // work with JSON object

            final JsonObject obj = (JsonObject) item;

            // check if it is enabled

            if (!obj.getBoolean(TenantConstants.FIELD_ENABLED, true)) {
                span.log(Map.of(
                        Fields.EVENT, "log",
                        Fields.MESSAGE, "Trust anchor is disabled, will be removed."));
                continue;
            }

            // check for valid timestamps and timespan

            var notBefore = obj.getString("not-before");
            var notAfter = obj.getString("not-after");
            if (!isValidTimespan(now, notBefore, notAfter)) {
                span.log(Map.of(
                        Fields.EVENT, "log",
                        Fields.MESSAGE,
                        String.format("Trust anchor is not in valid time period, will be removed (now: %s, notBefore: %s, notAfter: %s).", now, notBefore, notAfter)));
                continue;
            }

            // add to result

            if (result == null) {
                result = new JsonArray();
            }
            result.add(obj);

        }

        // return a JSON array, or "null" if we have no result

        return result;

    }

    /**
     *
     * @param now The current point in time.
     * @param notBeforeValue The "not-before" value, as a string.
     * @param notAfterValue The "not-after" value, as a string.
     *
     * @return {@code true} if the trust anchor is valid, {@code false} otherwise.
     */
    protected static boolean isValidTimespan(final Instant now, final String notBeforeValue, final String notAfterValue) {

        try {

            var notBefore = Instant.parse(notBeforeValue);
            var notAfter = Instant.parse(notAfterValue);

            return now.isAfter(notBefore) && now.isBefore(notAfter);

        } catch (Exception e) {

            log.info("Failed to validate trust anchor", e);
            return false;

        }

    }

}
