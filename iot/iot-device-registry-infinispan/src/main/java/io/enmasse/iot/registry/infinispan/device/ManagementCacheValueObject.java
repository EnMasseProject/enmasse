package io.enmasse.iot.registry.infinispan.device;

import io.enmasse.iot.registry.infinispan.credentials.CredentialsKey;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.Serializable;
import java.util.*;
import org.eclipse.hono.service.management.credentials.CommonCredential;
import org.eclipse.hono.util.CredentialsConstants;

public class ManagementCacheValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    // A Json Object containing the registration information.
    private String devinceInfo;

    // resource version for the device registration info
    private String version;

    // A Json Array containing the credentials objects for this device.
    private String credentials;

    public ManagementCacheValueObject(String devinceInfo) {
        this.devinceInfo = devinceInfo;
        this.version = UUID.randomUUID().toString();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    /**
     * Tests if this version matches the provided version.
     *
     * @param resourceVersion The provided version to check, may be {@link Optional#empty()}.
     * @return {@code true} if the provided version to check is empty or matches the current version, {@code false}
     *         otherwise.
     */
    public boolean isVersionMatch(final Optional<String> resourceVersion) {

        Objects.requireNonNull(resourceVersion);
        return resourceVersion.isEmpty() || resourceVersion.get().equals(this.version);
    }

    public List<CredentialsKey> getCredentialsKeys(final String tenantId){

        final List<JsonObject> creds = new JsonArray(credentials).getList();
        final List<CredentialsKey> list = new ArrayList<>();

        for(JsonObject cred : creds){
            list.add(new CredentialsKey(
                            tenantId,
                            cred.getString(CredentialsConstants.FIELD_AUTH_ID),
                            cred.getString(CredentialsConstants.FIELD_TYPE)));
        }

        return list;
    }

    public String getDevinceInfo() {
        return devinceInfo;
    }

    public JsonObject getDevinceInfoAsJson() {
        return new JsonObject(devinceInfo);
    }

    public List<CommonCredential> getCredentialsList(){
        JsonArray credsArray = new JsonArray(credentials);
        final ArrayList<CommonCredential> result = new ArrayList<>();

        for (Object cred : credsArray.getList()) {
            result.add(((JsonObject) cred).mapTo(CommonCredential.class));
        }
        return result;
    }

    public JsonObject getCredential(final String authId, final String type) {
        for (Object entry : new JsonArray(credentials).getList()) {
            JsonObject cred = (JsonObject) entry;
            if ( cred.getString(CredentialsConstants.FIELD_TYPE) == type
                && cred.getString(CredentialsConstants.FIELD_AUTH_ID ) == authId){
                return cred;
            }
        }
        return null;
    }
}
