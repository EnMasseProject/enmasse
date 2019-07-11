/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.util;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public final class MoreFutures {
    private MoreFutures() {}

    public static <T> void completeHandler(final CompletableFuture<T> future, final Handler<AsyncResult<T>> handler) {
        future.whenComplete((result, error) -> {
            if (error == null) {
                handler.handle(Future.succeededFuture(result));
            } else {
                handler.handle(Future.failedFuture(error));
            }
        });
    }

}
