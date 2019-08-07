package io.enmasse.iot.registry.infinispan.device;

import io.enmasse.iot.registry.infinispan.CacheProvider;
import io.enmasse.iot.registry.infinispan.credentials.CredentialsKey;
import io.opentracing.Span;
import io.opentracing.noop.NoopSpan;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import static java.net.HttpURLConnection.*;
import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.service.management.Id;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.device.Device;
import org.eclipse.hono.service.management.device.DeviceManagementService;
import org.eclipse.hono.service.registration.AbstractRegistrationService;
import org.eclipse.hono.service.registration.RegistrationService;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsResult;
import org.eclipse.hono.util.RegistrationResult;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

// TODO : add authorisation calls
// TODO : add logging
@Repository
@Qualifier("serviceImpl")
@Primary
public class DevicesCredentialsCacheService implements CredentialsManagementService, DeviceManagementService,
        CredentialsService, RegistrationService {

    // Adapter cache :
    // <( tenantId + authId + type), (credential + deviceId + sync-flag + registration data version)>
    private RemoteCache<CredentialsKey, AdapterCacheValueObject> adapterCache;
    // Management cache
    // <(TenantId+DeviceId), (Device information + version + credentials)>
    private RemoteCache<RegistrationKey, ManagementCacheValueObject> managementCache;

    /**
     * Registration service, based on {@link AbstractRegistrationService}.
     * <p>
     * This helps work around Java's inability to inherit from multiple base classes. We create a new Registration
     * service, overriding the implementation of {@link AbstractRegistrationService} with the implementation of our
     * {@link DevicesCredentialsCacheService#getDevice(String, String, Handler)}.
     */
    private final AbstractRegistrationService registrationService = new AbstractRegistrationService() {

        @Override
        public void getDevice(final String tenantId, final String deviceId,
                final Handler<AsyncResult<RegistrationResult>> resultHandler) {
            DevicesCredentialsCacheService.this.getDevice(tenantId, deviceId, resultHandler);
        }
    };


    @Autowired
    public DevicesCredentialsCacheService (final CacheProvider provider) {
        this.adapterCache = provider.getOrCreateCache("adaperCredentialsCache");
        this.managementCache = provider.getOrCreateCache("managementCredentialsCache");
    }

    //fixme : log or span ?
    private static final Logger log = LoggerFactory.getLogger(DevicesCredentialsCacheService.class);

    // AMQP API

    @Override
    public void get(String tenantId, String type, String authId, Span span,
            Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        get(tenantId, type, authId, null, span, resultHandler);
    }

    @Override
    //fixme nested async calls
    public void get(String tenantId, String type, String authId, JsonObject clientContext, Span span,
            Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        final CredentialsKey key = new CredentialsKey(tenantId, authId, type);

        adapterCache.getAsync(key).thenAccept(credential -> {
            if (credential != null) {

                if (! credential.isInSync()) {
                    final RegistrationKey mgmtKey = new RegistrationKey(tenantId, credential.getDeviceId());
                    ManagementCacheValueObject mgmtCred = managementCache.get(mgmtKey);
                    if (mgmtCred.isVersionMatch(Optional.of(credential.getManagementCacheExpectedVersion()))){
                        credential.setInSync(true);
                        adapterCache.putAsync(key, credential);

                        verifyContextAndReturn(tenantId, authId, type, credential, clientContext, resultHandler);
                    } else {
                        log.debug("Credential not in synced [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId, type);
                        resultHandler.handle(Future.succeededFuture(CredentialsResult.from(HTTP_INTERNAL_ERROR)));
                    }
                } else {
                    verifyContextAndReturn(tenantId, authId, type, credential, clientContext, resultHandler);
                }
            } else {
                    log.debug("Credential not found [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId, type);
                    resultHandler.handle(Future.succeededFuture(CredentialsResult.from(HTTP_NOT_FOUND)));
            }
        });
    }

    private void verifyContextAndReturn(final String tenantId, final String authId, final String type,
            final AdapterCacheValueObject credential, final JsonObject clientContext,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        if (clientContext != null && !clientContext.isEmpty()) {
            if (contextMatches(clientContext, new JsonObject(credential.getCrentential()))) {
                log.debug("Retrieve credential, context matches [tenant-id: {}, auth-id: {}, type: {}]",
                        tenantId,
                        authId, type);
                resultHandler.handle(Future.succeededFuture(
                        CredentialsResult.from(HTTP_OK, new JsonObject(credential.getCrentential()))));
            } else {
                log.debug("Context mismatch [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId,
                        type);
                resultHandler.handle(Future.succeededFuture(CredentialsResult.from(HTTP_NOT_FOUND)));
            }
        } else {
            resultHandler.handle(Future.succeededFuture(
                    CredentialsResult.from(HTTP_OK, new JsonObject(credential.getCrentential()))));
        }
    }

    // MANAGEMENT API

    @Override
    //fixme : nested Async call
    public void set(String tenantId, String deviceId, Optional<String> resourceVersion,
            List<CommonCredential> credentials, Span span, Handler<AsyncResult<OperationResult<Void>>> resultHandler) {

        final RegistrationKey regKey = new RegistrationKey(tenantId, deviceId);

        managementCache.getAsync(regKey).thenAccept(device -> {
                if (device != null) {
                    if (device.isVersionMatch(resourceVersion)) {

                        //remove entries in adapter cache
                        final List<CredentialsKey> keysList = device.getCredentialsKeys(tenantId);
                        for (CredentialsKey key : keysList) {
                            adapterCache.removeAsync(key);
                        }

                        // update version and credentials in management cache
                        final String newVer = UUID.randomUUID().toString();

                        device.setVersion(newVer);
                        device.setCredentials(JsonObject.mapFrom(credentials).encode());
                        managementCache.putAsync(regKey, device);

                        // create entries in adapter cache
                        for (CommonCredential cred : credentials) {
                            CredentialsKey credKey = new CredentialsKey(
                                    tenantId,
                                    cred.getAuthId(),
                                    JsonObject.mapFrom(cred).getString(CredentialsConstants.FIELD_TYPE));

                            AdapterCacheValueObject val = new AdapterCacheValueObject(
                                    JsonObject.mapFrom(cred).encode(),
                                    deviceId,
                                    newVer);

                            adapterCache.put(credKey, val);
                        }
                    } else {
                        resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                    }
                } else {
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
                }
        });
    }

    @Override
    //fixme : nested Async calls
    public void get(String tenantId, String deviceId, Span span,
            Handler<AsyncResult<OperationResult<List<CommonCredential>>>> resultHandler) {

        final RegistrationKey regKey = new RegistrationKey(tenantId, deviceId);

        managementCache.getAsync(regKey).thenAccept( mgmtObject -> {
            if (mgmtObject != null) {
                resultHandler.handle(Future.succeededFuture(
                    OperationResult.ok(HTTP_OK,
                            mgmtObject.getCredentialsList(),
                            Optional.empty(),
                            Optional.of(mgmtObject.getVersion()))));
            } else {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            }
        });
    }



    @Override
    public void createDevice(String tenantId, Optional<String> optionalDeviceId, Device device, Span span,
            Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final String deviceId = optionalDeviceId.orElseGet(() -> generateDeviceId(tenantId));

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);
        final ManagementCacheValueObject val = new ManagementCacheValueObject(JsonObject.mapFrom(device).encode());

        managementCache.putIfAbsentAsync(key, val).thenAccept(result -> {
                if ( result == null){
                    System.out.println("sucess");
                    resultHandler.handle(Future.succeededFuture(
                            OperationResult.ok(HTTP_CREATED,
                                    Id.of(deviceId),
                                    Optional.empty(), Optional.of(val.getVersion()))));
                } else {
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                }
        });
    }

    @Override
    public void readDevice(String tenantId, String deviceId, Span span,
            Handler<AsyncResult<OperationResult<Device>>> resultHandler) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);
        managementCache.getAsync(key).thenAccept(res -> {
           if (res != null) {
               resultHandler.handle(Future.succeededFuture(
                       OperationResult.ok(HTTP_OK,
                               res.getDevinceInfoAsJson().mapTo(Device.class),
                               Optional.empty(),
                               Optional.of(res.getVersion()))));
           } else {
               resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
           }
        });
    }

    //fixme : remove lazyness
    private void readADevice(String tenantId, String deviceId, Span span,
            Handler<AsyncResult<RegistrationResult>> resultHandler) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);
        managementCache.getAsync(key).thenAccept(res -> {
            if (res != null) {
                resultHandler.handle(Future.succeededFuture(
                        RegistrationResult.from(HTTP_OK,
                                res.getDevinceInfoAsJson())));
            } else {
                resultHandler.handle(Future.succeededFuture(RegistrationResult.from(HTTP_NOT_FOUND)));
            }
        });
    }

    @Override
    //fixme : nested Async call
    public void updateDevice(String tenantId, String deviceId, Device device,
            Optional<String> resourceVersion, Span span, Handler<AsyncResult<OperationResult<Id>>> resultHandler) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);

        managementCache.getAsync(key).thenAccept(result -> {
            if ( result != null){

                if (result.isVersionMatch(resourceVersion)){
                    final String newVersion = UUID.randomUUID().toString();
                    result.getCredentialsKeys(tenantId).forEach(credKey -> {
                        //fixme : is there a "get, process, update method?
                       adapterCache.getAsync(credKey).thenAccept(adapterVal -> {
                           adapterVal.setInSync(false);
                           adapterVal.setManagementCacheExpectedVersion(newVersion);
                           adapterCache.put(credKey, adapterVal);
                       });
                    });
                    // fixme : do it when the previous operations are done.
                    managementCache.put(key, new ManagementCacheValueObject(JsonObject.mapFrom(device).encode()));
                } else {
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                }
            } else {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            }
        });
    }

    @Override
    //fixme : 2 nested Async calls
    public void deleteDevice(String tenantId, String deviceId, Optional<String> resourceVersion, Span span,
            Handler<AsyncResult<Result<Void>>> resultHandler) {

        final RegistrationKey key = new RegistrationKey(tenantId, deviceId);

        managementCache.getAsync(key).thenAccept(result -> {
            if ( result != null){

                if (result.isVersionMatch(resourceVersion)){
                    result.getCredentialsKeys(tenantId).forEach(credKey -> {
                            adapterCache.removeAsync(credKey).thenAccept(r -> {
                                managementCache.remove(key);
                            });
                });
                } else {
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                }
            } else {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
            }

        });
    }

    // DEVICES

    public void assertRegistration(final String tenantId, final String deviceId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        registrationService.assertRegistration(tenantId, deviceId, resultHandler);
    }

    public void assertRegistration(final String tenantId, final String deviceId, final String gatewayId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {
        registrationService.assertRegistration(tenantId, deviceId, gatewayId, resultHandler);
    }

    private void getDevice(final String tenantId, final String deviceId,
            final Handler<AsyncResult<RegistrationResult>> resultHandler) {

        readADevice(tenantId, deviceId, NoopSpan.INSTANCE, resultHandler);
    }
    // PRIVATE UTIL METHODS

    private static boolean contextMatches(final JsonObject clientContext, final JsonObject storedCredential) {
        final AtomicBoolean match = new AtomicBoolean(true);
        clientContext.forEach(field -> {
            if (storedCredential.containsKey(field.getKey())) {
                if (!storedCredential.getString(field.getKey()).equals(field.getValue())) {
                    match.set(false);
                }
            } else {
                match.set(false);
            }
        });
        return match.get();
    }

    /**
     * Generate a random device ID.
     */
    private String generateDeviceId(final String tenantId) {

        String tempDeviceId;
        do {
            tempDeviceId = UUID.randomUUID().toString();
        } while (managementCache.containsKey(new RegistrationKey(tenantId, tempDeviceId)));
        return tempDeviceId;
    }
}



