/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.fabric8.kubernetes.client.utils;

import io.fabric8.kubernetes.client.RequestConfig;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

import static io.fabric8.kubernetes.client.utils.Utils.isNotNullOrEmpty;

public class ImpersonatorInterceptor implements Interceptor {
  private final RequestConfig requestConfig;
  private static String impersonateUser = null;

  public static void setImpersonateUser(String user) {
      impersonateUser = user;
  }

  public ImpersonatorInterceptor(RequestConfig config) {
    this.requestConfig = config;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (isNotNullOrEmpty(impersonateUser)) {
      Request.Builder requestBuilder = chain.request().newBuilder();

      requestBuilder.addHeader("Impersonate-User", impersonateUser);

      if (isNotNullOrEmpty(requestConfig.getImpersonateGroup())) {
        requestBuilder.addHeader("Impersonate-Group", requestConfig.getImpersonateGroup());
      }

      if (isNotNullOrEmpty(requestConfig.getImpersonateExtras())) {
        for (Map.Entry<String, String> entry : requestConfig.getImpersonateExtras().entrySet()) {
          requestBuilder.addHeader("Impersonate-Extra-" + entry.getKey(), entry.getValue());
        }
      }

      request = requestBuilder.build();
    }
    return chain.proceed(request);
  }
}
