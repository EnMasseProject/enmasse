/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public final class MoreFutures {

    private static final Logger log = LoggerFactory.getLogger(MoreFutures.class);

    private MoreFutures() {}

    public static <T> void completeHandler(final Supplier<CompletableFuture<T>> supplier, final Handler<AsyncResult<T>> handler) {
        if (supplier == null) {
            handler.handle(Future.failedFuture(new NullPointerException("'future' to handle must not be 'null'")));
        }

        final CompletableFuture<T> future;
        try {
            future = supplier.get();
        } catch (final Exception e) {
            log.debug("Failed to prepare future", e);
            handler.handle(Future.failedFuture(e));
            return;
        }

        future.whenComplete((result, error) -> {
            log.debug("Result - {}", result, error);
            if (error == null) {
                handler.handle(Future.succeededFuture(result));
            } else {
                log.debug("Future failed", error);
                if (error instanceof CompletionException) {
                    error = error.getCause();
                }
                handler.handle(Future.failedFuture(error));
            }
        });
    }

    public static CompletableFuture<Void> allOf(final List<CompletableFuture<?>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public static <T> CompletableFuture<T> map(final Future<T> future) {

        final CompletableFuture<T> result = new CompletableFuture<>();

        future.setHandler(ar -> {
            if (ar.succeeded()) {
                result.complete(ar.result());
            } else {
                result.completeExceptionally(ar.cause());
            }
        });

        return result;
    }

}
