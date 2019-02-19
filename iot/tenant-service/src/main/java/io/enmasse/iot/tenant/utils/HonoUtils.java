/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.utils;

import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

public final class HonoUtils {

    private HonoUtils() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Future<Void> toVoidResult(final List<Future<?>> deploymentTracker) {
        final Future<Void> result = Future.future();
        CompositeFuture.all((List)deploymentTracker)
                .setHandler(r -> {
                    if (r.failed()) {
                        result.fail(r.cause());
                    } else {
                        result.complete(null);
                    }
                });
        return result;
    }

}
