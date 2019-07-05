/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;

import io.opentracing.Span;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.eclipse.hono.auth.BCryptHelper;
import org.eclipse.hono.service.credentials.CredentialsService;
import org.eclipse.hono.service.management.OperationResult;
import org.eclipse.hono.service.management.Result;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.service.management.credentials.CredentialsManagementService;
import org.eclipse.hono.service.management.credentials.PasswordCredential;
import org.eclipse.hono.service.management.credentials.PasswordSecret;
import org.eclipse.hono.tracing.TracingHelper;
import org.eclipse.hono.util.CredentialsConstants;
import org.eclipse.hono.util.CredentialsResult;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A credentials service that use an Infinispan as a backend service.
 * Infinispan is an open source project providing a distributed in-memory key/value data store
 *
 * <p>
 *@see <a href="https://infinspan.org">https://infinspan.org</a>
 *
 */
@Repository
@Primary
@Qualifier("serviceImpl")
public class CacheCredentialService extends AbstractVerticle
        implements CredentialsManagementService, CredentialsService  {

    private final RemoteCache<CredentialsKey, RegistryCredentialObject> credentialsCache;
    private final RemoteCache<RegistrationKey, String> versions;

    private CacheCredentialConfigProperties config;
    private static final Logger log = LoggerFactory.getLogger(CacheCredentialService.class);

    /**
     * Creates a new service instance for a password encoder.
     *
     * @throws NullPointerException if encoder is {@code null}.
     */
    @Autowired
    protected CacheCredentialService(final RemoteCache<CredentialsKey,RegistryCredentialObject> cache,
            final RemoteCache<RegistrationKey, String> versions) {
        this.credentialsCache = cache;
        this.versions = versions;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The result object will include a <em>no-cache</em> directive.
     */
    @Override
    public void get(final String tenantId, final String type, final String authId, final Span span,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {
        get(tenantId, type, authId, null, span, resultHandler);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The result object will include a <em>no-cache</em> directive.
     */
    @Override
    public void get(
            final String tenantId,
            final String type,
            final String authId,
            final JsonObject clientContext,
            final Span span,
            final Handler<AsyncResult<CredentialsResult<JsonObject>>> resultHandler) {

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(authId);
        Objects.requireNonNull(resultHandler);

        final CredentialsKey key = new CredentialsKey(tenantId, authId, type);

        credentialsCache.getAsync(key).thenAccept(credential -> {
            if (credential == null) {
                log.debug("Credential not found [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId, type);
                resultHandler.handle(Future.succeededFuture(CredentialsResult.from(HTTP_NOT_FOUND)));
            } else if (clientContext != null && !clientContext.isEmpty()) {
                if (contextMatches(clientContext, new JsonObject(credential.getOriginalJson()))) {
                    log.debug("Retrieve credential, context matches [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId, type);
                    resultHandler.handle(Future.succeededFuture(
                            CredentialsResult.from(HTTP_OK,
                                    new JsonObject(credential.getOriginalJson()))));
                } else {
                    log.debug("Context mismatch [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId, type);
                    resultHandler.handle(Future.succeededFuture(CredentialsResult.from(HTTP_NOT_FOUND)));
                }
            } else {
                log.debug("Retrieve credential [tenant-id: {}, auth-id: {}, type: {}]", tenantId, authId, type);
                resultHandler.handle(Future.succeededFuture(
                        CredentialsResult.from(HTTP_OK,
                                new JsonObject(credential.getOriginalJson()))));
            }
        });
    }

    @Override
    //TODO span
    // todo pull some code in soe static method
    public void set(final String tenantId, final String deviceId, final Optional<String> resourceVersion,
            final List<CommonCredential> credentials, final Span span, final Handler<AsyncResult<OperationResult<Void>>> resultHandler) {

        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(resourceVersion);
        Objects.requireNonNull(credentials);

        for (CommonCredential credential : credentials){
            try {
                checkCredential(credential);
            } catch (final IllegalStateException e) {
                resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_BAD_REQUEST)));
                return;
            }
        }

        final Future<Boolean> versionCheck = Future.future();
        resourceVersion.ifPresentOrElse( version -> {
            RegistrationKey versionKey = new RegistrationKey(tenantId, deviceId);
            versions.getAsync(versionKey).thenAccept(storedVersion -> {
                if (! version.equals(storedVersion)){
                    TracingHelper.logError(span, "Resource version mismatch");
                    resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_PRECON_FAILED)));
                    versionCheck.complete(false);
                }
            });
        }, () -> versionCheck.complete(true));

        //todo a better way to compose / chain this ? don't do if the future is failed instead of checking the value ?
        // todo how to return ???
        versionCheck.setHandler(doUpdate -> {

            if(doUpdate.result()) {
                // delete all credentials if any exist.
                removeAllCredentialsForDevice(tenantId, deviceId);

                // authId->credentials[]
                final Map<String, JsonArray> credentialsForTenant = queryAllCredentialsForTenant(tenantId);

                // The data we'll add in the cache
                final Map<CredentialsKey, RegistryCredentialObject> credsToSave = new HashMap<>() ;

                // now add the new ones
                for (final CommonCredential credential : credentials) {

                    final String authId = credential.getAuthId();
                    final JsonObject credentialObject = JsonObject.mapFrom(credential);
                    final String type = credentialObject.getString(CredentialsConstants.FIELD_TYPE);
                    final var json = createOrGetAuthIdCredentials(authId, credentialsForTenant);

                    // find credentials - matching by type
                    JsonObject credentialsJson = json.stream()
                            .filter(JsonObject.class::isInstance).map(JsonObject.class::cast)
                            .filter(j -> type.equals(j.getString(CredentialsConstants.FIELD_TYPE)))
                            .findAny().orElse(null);

                    if (credentialsJson == null) {
                        // did not found an entry, add new one
                        credentialsJson = new JsonObject();
                        credentialsJson.put(CredentialsConstants.FIELD_AUTH_ID, authId);
                        credentialsJson.put(CredentialsConstants.FIELD_PAYLOAD_DEVICE_ID, deviceId);
                        credentialsJson.put(CredentialsConstants.FIELD_TYPE, type);
                        credentialsJson.put(CredentialsConstants.FIELD_ENABLED, credential.getEnabled());
                    }

                    if (!deviceId.equals(credentialsJson.getString(CredentialsConstants.FIELD_PAYLOAD_DEVICE_ID))) {
                        // found an entry for another device, with the same auth-id
                        TracingHelper.logError(span, "Auth-id already used for another device");
                        resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_CONFLICT)));
                    } else {

                        // save the entries to update

                        var secretsJson = credentialsJson.getJsonArray(CredentialsConstants.FIELD_SECRETS);
                        if (secretsJson == null) {
                            // secrets field was missing, assign
                            secretsJson = new JsonArray();
                            credentialsJson.put(CredentialsConstants.FIELD_SECRETS, secretsJson);
                        }
                        secretsJson.addAll(credentialObject.getJsonArray(CredentialsConstants.FIELD_SECRETS));

                        for (final Map.Entry<String, Object> entry : credential.getExtensions().entrySet()) {
                            credentialsJson.put(entry.getKey(), entry.getValue());
                        }
                        credentialsJson.remove(CredentialsConstants.FIELD_EXT);

                        credsToSave.put(
                                new CredentialsKey(tenantId, authId, type),
                                new RegistryCredentialObject(deviceId, tenantId, credentialsJson));
                    }
                }

                //update version
                final String newVersion = String.valueOf(credentials.hashCode());
                versions.putAsync(new RegistrationKey(tenantId, deviceId), newVersion).thenAccept(version -> {

                    // save to the cache
                    credentialsCache.putAllAsync(credsToSave)
                            .thenAccept(result -> {
                                resultHandler.handle(Future.succeededFuture(OperationResult.ok(
                                        HTTP_NO_CONTENT,
                                        null,
                                        Optional.empty(),
                                        Optional.of(newVersion))));
                            });
                });
            }
        });
    }

    @Override
    // todo span
    public void get(final String tenantId, final String deviceId, final Span span,
            final Handler<AsyncResult<OperationResult<List<CommonCredential>>>> resultHandler) {

        final List<CommonCredential>  creds = new ArrayList<>();

        queryAllCredentialsForDevice(tenantId, deviceId).forEach(result -> {
            creds.add(new JsonObject(result.getOriginalJson()).mapTo(CommonCredential.class));
        });

        if (creds.isEmpty()) {
            log.debug("Cannot retrieve credentials for device : not found [tenant-id: {}, deviceID {}]", tenantId, deviceId);
            resultHandler.handle(Future.succeededFuture(OperationResult.empty(HTTP_NOT_FOUND)));
        } else {

            log.debug("Retrieved {} credentials for device [tenant-id: {}, deviceID {}]", creds.size(), tenantId, deviceId);

            versions.getAsync(new RegistrationKey(tenantId, deviceId)).thenAccept(version -> {
                resultHandler.handle(Future.succeededFuture(
                        OperationResult.ok(HTTP_OK,
                                creds,
                                //TODO cache directive,
                                Optional.empty(),
                                Optional.ofNullable(version))));
            });
        }
    }

    /**
     * Remove all the credentials for the given device ID.
     * @param tenantId the Id of the tenant which the device belongs to.
     * @param deviceId the id of the device that is deleted.
     * @param span The active OpenTracing span for this operation.
     * @param resultHandler the operation result.
     */
    public void remove(final String tenantId, final String deviceId,
            final Span span, final Handler<AsyncResult<Result<Void>>> resultHandler) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(resultHandler);

        log.debug("removing credentials for device [tenant-id: {}, device-id: {}]", tenantId, deviceId);

        resultHandler.handle(Future.succeededFuture(removeAllCredentialsForDevice(tenantId, deviceId)));
    }

    private Result<Void> removeAllCredentialsForDevice(final String tenantId, final String deviceId) {

        final List<RegistryCredentialObject>  matches = queryAllCredentialsForDevice(tenantId, deviceId);
        if ( matches.isEmpty()) {
            return Result.from(HTTP_NOT_FOUND);
        } else {
            final List<CompletableFuture<RegistryCredentialObject>> futureResultList = new ArrayList<>();
            matches.forEach(registryCredential -> {
                final CredentialsKey key = new CredentialsKey(
                        tenantId,
                        new JsonObject(registryCredential.getOriginalJson()).getString(CredentialsConstants.FIELD_AUTH_ID),
                        new JsonObject(registryCredential.getOriginalJson()).getString(CredentialsConstants.FIELD_TYPE));
                futureResultList.add( credentialsCache.removeAsync(key));
            });
            CompletableFuture.allOf(futureResultList.toArray(new CompletableFuture[futureResultList.size()])).thenAccept( r-> {
                log.debug("Removed {} credentials for device [tenant-id: {}, deviceID {}]", matches.size(), tenantId, deviceId);
            });
            return Result.from(HTTP_NO_CONTENT);
        }
    }

    // TODO : async request
    private List<RegistryCredentialObject> queryAllCredentialsForDevice(final String tenantId, final String deviceId){
        // Obtain a query factory for the cache
        final QueryFactory queryFactory = Search.getQueryFactory(credentialsCache);
        final Query query = queryFactory.from(RegistryCredentialObject.class)
                .having("deviceId").eq(deviceId)
                .and().having("tenantId").eq(tenantId)
                .build();
        // Execute the query
        return query.list();
    }

    // TODO : async request
    private Map<String, JsonArray> queryAllCredentialsForTenant(final String tenantId){
        // Obtain a query factory for the cache
        final QueryFactory queryFactory = Search.getQueryFactory(credentialsCache);
        final Query query = queryFactory.from(RegistryCredentialObject.class)
                .having("tenantId").eq(tenantId)
                .build();

        final List<RegistryCredentialObject> result = query.list();
        final Map<String, JsonArray> credentialsForTenantMap = new HashMap<>();

        for (RegistryCredentialObject cred : result) {
            credentialsForTenantMap.compute(cred.getAuthId(),
                    (key, val) -> {
                        if (val == null) {
                            return new JsonArray().add(new JsonObject(cred.getOriginalJson()));
                        } else {
                            return val.add(new JsonObject(cred.getOriginalJson()));
                        }
            });
        }
        return credentialsForTenantMap;
    }

    private static JsonArray createOrGetAuthIdCredentials(final String authId,
            final Map<String, JsonArray> credentialsForTenant) {
        return credentialsForTenant.computeIfAbsent(authId, id -> new JsonArray());
    }

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
     * Validate a secret.
     *
     * @param credential The secret to validate.
     * @throws IllegalStateException if the secret is not valid.
     */
    protected void checkCredential(final CommonCredential credential) {
        credential.checkValidity();
        if (credential instanceof PasswordCredential) {
            for (final PasswordSecret passwordSecret : ((PasswordCredential) credential).getSecrets()) {
                checkHashedPassword(passwordSecret);
                switch (passwordSecret.getHashFunction()) {
                case CredentialsConstants.HASH_FUNCTION_BCRYPT:
                    final String pwdHash = passwordSecret.getPasswordHash();
                    verifyBcryptPasswordHash(pwdHash);
                    break;
                default:
                    // pass
                }
                // pass
            }
        }
    }

    /**
     * Verifies that a hash value is a valid BCrypt password hash.
     * <p>
     * The hash must be a version 2a hash and must not use more than the configured
     * maximum number of iterations as returned by {@link #getMaxBcryptIterations()}.
     *
     * @param pwdHash The hash to verify.
     * @throws IllegalStateException if the secret does not match the criteria.
     */
    protected void verifyBcryptPasswordHash(final String pwdHash) {

        Objects.requireNonNull(pwdHash);
        if (BCryptHelper.getIterations(pwdHash) > getMaxBcryptIterations()) {
            throw new IllegalStateException("password hash uses too many iterations, max is " + getMaxBcryptIterations());
        }
    }

    private static void checkHashedPassword(final PasswordSecret secret) {
        if (secret.getHashFunction() == null) {
            throw new IllegalStateException("missing/invalid hash function");
        }

        if (secret.getPasswordHash() == null) {
            throw new IllegalStateException("missing/invalid password hash");
        }
    }

    protected int getMaxBcryptIterations() {
        //fixme
        //return getConfig().getMaxBcryptIterations();
        return 10;
    }

    /**
     * Removes all credentials from the registry.
     */
    public void clear() {
        credentialsCache.clear();
        versions.clear();
    }
}
