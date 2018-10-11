/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import com.google.common.collect.Sets;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

public class AdminApiClient extends ApiClient {
    protected static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;
    private final String addressSpacePlansPath;
    private final String addressPlansPath;

    public AdminApiClient(Kubernetes kubernetes) {
        super(kubernetes, kubernetes.getRestEndpoint(), "admin.enmasse.io/v1alpha1");
        this.addressSpacePlansPath = String.format("/apis/admin.enmasse.io/v1alpha1/namespaces/%s/addressspaceplans", kubernetes.getNamespace());
        this.addressPlansPath = String.format("/apis/admin.enmasse.io/v1alpha1/namespaces/%s/addressplans", kubernetes.getNamespace());
    }

    public void close() {
        client.close();
        vertx.close();
    }

    @Override
    protected String apiClientName() {
        return "AdminApi";
    }

    public AddressSpacePlan getAddressSpacePlan(String name) throws Exception {
        JsonObject spacePlan = getResource("address-space-plan", addressSpacePlansPath, name);
        return AddressSpacePlan.fromJson(spacePlan, this);
    }

    public void createAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        createResource("address-space-plan", addressSpacePlansPath, addressSpacePlan.toJson());
    }

    public void replaceAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        replaceResource("address-space-plan", addressSpacePlansPath, addressSpacePlan.getName(), addressSpacePlan.toJson());
    }

    public void deleteAddressSpacePlan(AddressSpacePlan addressSpacePlan) throws Exception {
        deleteResource("address-space-plan", addressSpacePlansPath, addressSpacePlan.getName());
    }

    public AddressPlan getAddressPlan(String name) throws Exception {
        return AddressPlan.fromJson(getResource("address-plan", addressPlansPath, name));
    }

    public void createAddressPlan(AddressPlan addressPlan) throws Exception {
        createResource("address-plan", addressPlansPath, addressPlan.toJson());
    }

    public void replaceAddressPlan(AddressPlan addressPlan) throws Exception {
        replaceResource("address-plan", addressPlansPath, addressPlan.getName(), addressPlan.toJson());
    }

    public void deleteAddressPlan(AddressPlan addressPlan) throws Exception {
        deleteResource("address-plan", addressPlansPath, addressPlan.getName());
    }
}
