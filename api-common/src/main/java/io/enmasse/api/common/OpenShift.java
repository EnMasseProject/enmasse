/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.common;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

public class OpenShift {
    private static final Logger log = LoggerFactory.getLogger(OpenShift.class);
    public static final String ENMASSE_OPENSHIFT = "ENMASSE_OPENSHIFT";
    public static final String ENMASSE_OPENSHIFT4 = "ENMASSE_OPENSHIFT4";

    public static boolean isOpenShift(KubernetesClient client) {
        if (System.getenv().containsKey(ENMASSE_OPENSHIFT)) {
            return Boolean.parseBoolean(System.getenv().get(ENMASSE_OPENSHIFT));
        }
        int retries = 10;
        while (true) {
            // Need to query the full API path because Kubernetes does not allow GET on /
            OkHttpClient httpClient = client.adapt(OkHttpClient.class);
            HttpUrl url = HttpUrl.get(client.getMasterUrl()).resolve("/apis/route.openshift.io");
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                return response.code() >= 200 && response.code() < 300;
            } catch (IOException e) {
                retries--;
                if (retries <= 0) {
                    throw new UncheckedIOException(e);
                } else {
                    log.warn("Exception when checking API availability, retrying {} times", retries, e);
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public static boolean isOpenShift4(KubernetesClient client) {
        if (!isOpenShift(client)) {
            return false;
        }
        if (System.getenv().containsKey(ENMASSE_OPENSHIFT4)) {
            return Boolean.parseBoolean(System.getenv().get(ENMASSE_OPENSHIFT4));
        }

        int retries = 10;
        while (true) {
            try {
                VersionInfo version = client.getVersion();
                if (version == null) {
                    // We got an non error response, but there is no version info available from the endpoint. Assume
                    // OpenShift < 4.
                    return false;
                }
                return isOpenShift4(version);
            } catch (KubernetesClientException e) {
                retries--;
                if (retries <= 0) {
                    throw new RuntimeException(e);
                } else {
                    log.warn("Exception when checking API availability, retrying {} times", retries, e);
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    static boolean isOpenShift4(VersionInfo version) {

        if (version == null) {
            return false;
        }

        String major = version.getMajor();
        String minor = version.getMinor();

        int majorInt = 0;
        int minorInt = 0;
        try {
            majorInt = Integer.parseInt(String.valueOf(major));
            minorInt = Integer.parseInt(String.valueOf(minor).replaceAll("\\+*$", ""));
        } catch (NumberFormatException e) {
            return false;
        }

        return majorInt == 1 && minorInt > 11;
    }

}
