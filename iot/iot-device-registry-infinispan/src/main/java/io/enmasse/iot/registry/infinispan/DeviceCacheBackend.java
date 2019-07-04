package io.enmasse.iot.registry.infinispan;

import com.google.common.base.MoreObjects;
import io.opentracing.Span;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.service.management.device.DeviceBackend;
import org.eclipse.hono.util.CredentialsResult;
import org.eclipse.hono.util.RegistrationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * A device backend that keeps all data in memory but is backed by a file. This is done by leveraging and unifying
 * {@link CacheRegistrationService} and {@link CacheCredentialService}
 */
@Repository
@Qualifier("backend")
public class DeviceCacheBackend implements DeviceBackend {

    private final CacheRegistrationService registrationService;
    private final CacheCredentialService credentialsService;

    /**
     * Create a new instance.
     *
     * @param registrationService an implementation of registration service.
     * @param credentialsService an implementation of credentials service.
     */
    @Autowired
    public DeviceCacheBackend(
            @Qualifier("serviceImpl") final CacheRegistrationService registrationService,
            @Qualifier("serviceImpl") final CacheCredentialService credentialsService) {
        this.registrationService = registrationService;
        this.credentialsService = credentialsService;
    }

    // DEVICES

    @Override
    public void assertRegistration(final String tenantId, final String deviceId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        registrationService.assertRegistration(tenantId, deviceId, resultHandler);
    }

    @Override
    public void assertRegistration(final String tenantId, final String deviceId, final String gatewayId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        registrationService.assertRegistration(tenantId, deviceId, gatewayId, resultHandler);
    }

    @Override
    public void readDevice(final String tenantId, final String deviceId, final Span span,
            final Handler<AsyncResult<OperationResult<Device>>> resultHandler) {
        registrationService.readDevice(tenantId, deviceId, span, resultHandler);
    }

    @Override
    public void deleteDevice(final String tenantId, final String deviceId, final Optional<String> resourceVersion,
            final Span span, final Handler<AsyncResult<Result<Void>>> resultHandler) {

        final Future<Result<Void>> future = Future.future();
        registrationService.deleteDevice(tenantId, deviceId, resourceVersion, span, future);

        future.compose(r -> {
            if (r.getStatus() != HttpURLConnection.HTTP_NO_CONTENT) {
                return Future.succeededFuture(r);
            }

            // now delete the credentials set
            final Future<Result<Void>> f = Future.future();
            credentialsService.remove(
                    tenantId,
                    deviceId,
                    span,
                    f);

            // pass on the original result
            return f.map(r);
        })
                .setHandler(resultHandler);
    }

    @Override
    public void createDevice(final String tenantId, final Optional<String> deviceId, final Device device,
            final Span span, final Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final Future<OperationResult<Id>> future = Future.future();
        registrationService.createDevice(tenantId, deviceId, device, span, future);

        future
                .compose(r -> {

                    if (r.getStatus() != HttpURLConnection.HTTP_CREATED) {
                        return Future.succeededFuture(r);
                    }

                    // now create the empty credentials set
                    final Future<OperationResult<Void>> f = Future.future();
                    credentialsService.set(
                            tenantId,
                            r.getPayload().getId(),
                            Optional.empty(),
                            Collections.emptyList(),
                            span,
                            f);

                    // pass on the original result
                    return f.map(r);

                })

                .setHandler(resultHandler);

    }

    @Override
    public void updateDevice(final String tenantId, final String deviceId, final Device device,
            final Optional<String> resourceVersion, final Span span,
            final Handler<AsyncResult<OperationResult<Id>>> resultHandler) {
        registrationService.updateDevice(tenantId, deviceId, device, resourceVersion, span, resultHandler);
    }

    // CREDENTIALS

    @Override
    public final void get(final String tenantId, final String type, final String authId,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        credentialsService.get(tenantId, type, authId, resultHandler);
    }

    @Override
    public void get(final String tenantId, final String type, final String authId, final Span span,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        credentialsService.get(tenantId, type, authId, span, resultHandler);
    }

    @Override
    public final void get(final String tenantId, final String type, final String authId, final JsonObject clientContext,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        credentialsService.get(tenantId, type, authId, clientContext, resultHandler);
    }

    @Override
    public void get(final String tenantId, final String type, final String authId, final JsonObject clientContext,
            final Span span,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        credentialsService.get(tenantId, type, authId, clientContext, span, resultHandler);
    }

    @Override
    public void set(final String tenantId, final String deviceId, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span, final Handler<AsyncResult<OperationResult<Void>>> resultHandler) {
        //TODO check if device exists
        credentialsService.set(tenantId, deviceId, resourceVersion, credentials, span, resultHandler);
    }

    @Override
    public void get(final String tenantId, final String deviceId, final Span span,
            final Handler<AsyncResult<OperationResult<List<CommonCredential>>>> resultHandler) {

        final Future<OperationResult<List<CommonCredential>>> f = Future.future();
        credentialsService.get(tenantId, deviceId, span, f);
        f.compose(r -> {
            if (r.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
                final Future<OperationResult<Device>> readFuture = Future.future();
                registrationService.readDevice(tenantId, deviceId, span, readFuture);
                return readFuture.map(d -> {
                    if (d.getStatus() == HttpURLConnection.HTTP_OK) {
                        return OperationResult.ok(HttpURLConnection.HTTP_OK,
                                Collections.<CommonCredential> emptyList(),
                                r.getCacheDirective(),
                                r.getResourceVersion());
                    } else {
                        return r;
                    }
                });
            } else {
                return Future.succeededFuture(r);
            }
        }).setHandler(resultHandler);
    }

    /**
     * Removes all credentials from the registry.
     */
    public void clear() {
        registrationService.clear();
        credentialsService.clear();
    }

    /**
     * Creator for {@link MoreObjects.ToStringHelper}.
     *
     * @return A new instance for this instance.
     */
    protected MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("credentialsService", this.credentialsService)
                .add("registrationService", this.registrationService);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
