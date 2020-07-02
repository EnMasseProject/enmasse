/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.utils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public final class MoreFutures {

    private static final Logger log = LoggerFactory.getLogger(MoreFutures.class);

    private MoreFutures() {}

    public static <T> void finishHandler(final Supplier<Future<T>> supplier, final Handler<AsyncResult<T>> handler) {
        if (supplier == null) {
            handler.handle(Future.failedFuture(new NullPointerException("'future' to handle must not be 'null'")));
            return;
        }

        final Future<T> future;
        try {
            future = supplier.get();
        } catch (final Exception e) {
            log.info("Failed to prepare future", e);
            handler.handle(Future.failedFuture(e));
            return;
        }

        future.onComplete(ar -> {
            if (ar.failed()) {
                log.info("Future failed", ar.cause());
            }
            handler.handle(ar);
        });
    }

    public static <T> void completeHandler(final Supplier<CompletableFuture<T>> supplier, final Handler<AsyncResult<T>> handler) {
        if (supplier == null) {
            handler.handle(Future.failedFuture(new NullPointerException("'future' to handle must not be 'null'")));
            return;
        }

        final CompletableFuture<T> future;
        try {
            future = supplier.get();
        } catch (final Exception e) {
            log.info("Failed to prepare future", e);
            handler.handle(Future.failedFuture(e));
            return;
        }

        future.whenComplete((result, error) -> {
            log.debug("Result - {}", result, error);
            if (error == null) {
                handler.handle(Future.succeededFuture(result));
            } else {
                if (error instanceof CompletionException) {
                    error = error.getCause();
                }
                log.info("Future failed", error);
                handler.handle(Future.failedFuture(error));
            }
        });
    }

    public static CompletableFuture<Void> allOf(final List<CompletableFuture<?>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public static <T> Future<T> map(final CompletionStage<T> future) {

        if (future == null) {
            return null;
        }

        final Promise<T> result = Promise.promise();
        future.whenComplete((r, e) -> {
            if (e == null) {
                result.complete(r);
            } else {
                log.info("Operation failed", e);
                result.fail(e);
            }
        });

        return result.future();

    }

    public static <T> CompletableFuture<T> map(final Future<T> future) {

        if (future == null) {
            return null;
        }

        final CompletableFuture<T> result = new CompletableFuture<>();

        future.onComplete(ar -> {
            if (ar.succeeded()) {
                result.complete(ar.result());
            } else {
                log.info("Operation failed", ar.cause());
                result.completeExceptionally(ar.cause());
            }
        });

        return result;
    }

    /**
     * Use a {@link CompletableFuture} as a {@link Handler} for vertx.
     *
     * @param <T> The result type.
     * @param future The future to complete with the handler.
     * @return a handler which will complete the future.
     */
    public static <T> Handler<AsyncResult<T>> handler(final CompletableFuture<T> future) {
        return result -> {
            if (result.succeeded()) {
                future.complete(result.result());
            } else {
                log.info("Operation failed", result.cause());
                future.completeExceptionally(result.cause());
            }
        };
    }

    /**
     * Emulating {@link CompletableFuture#whenComplete(java.util.function.BiConsumer)} in vertx, and
     * work around eclipse-vertx/vert.x#3317.
     *
     * @param <T> The future type.
     * @param runnable The code to run.
     * @return A future with the same outcome as the input future, but which can be used to set another
     *         handler on.
     */
    public static <T> Future<T> whenComplete(final Future<T> future, final BiConsumer<T, Throwable> runnable) {

        final Future<T> nextFuture = Promise.<T>promise().future();

        future.onComplete(ar -> {
            try {
                runnable.accept(ar.result(), ar.cause());
            } catch (Exception e) {
                log.warn("Failed in whenComplete", e);
            }
            nextFuture.handle(ar);
        });

        return nextFuture;
    }

    public static <T> Future<T> whenComplete(final Future<T> future, final Runnable runnable) {
        return whenComplete(future, (r, e) -> runnable.run());
    }

    /**
     * Implement "get" operation on a vertx future.
     *
     * @param <T> The data type.
     * @param future The future to get from.
     * @return The value of the completed future.
     * @throws InterruptedException When the wait got interrupted.
     * @throws ExecutionException When the future failed.
     */
    public static <T> T get(final Future<T> future) throws InterruptedException, ExecutionException {
        try {
            return await(future, null);
        } catch (TimeoutException e) {
            // This can never happen as the TimeoutException is only thrown when we request a timeout.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Wait for the outcome of a "vertx" Future.
     *
     * @param <T> The data type.
     * @param future The future to wait for.
     * @param timeout The timeout. May be {@code null} if the method should wait forever.
     * @return The value of the completed future.
     * @throws InterruptedException When the wait got interrupted.
     * @throws ExecutionException When the future failed.
     * @throws TimeoutException When the wait timed out.
     */
    public static <T> T await(final Future<T> future, final Duration timeout) throws InterruptedException, ExecutionException, TimeoutException {

        final AtomicReference<AsyncResult<T>> result = new AtomicReference<>();
        final Semaphore sem = new Semaphore(0);
        future.onComplete(ar -> {
            result.set(ar);
            sem.release();
        });
        if (timeout == null) {
            sem.acquire();
        } else {
            final long millis = Math.max(timeout.toMillis(), 1);
            if (!sem.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Future did not complete after timeout period");
            }
        }

        final AsyncResult<T> ar = result.get();
        if (ar.failed()) {
            throw new ExecutionException(ar.cause());
        }

        return ar.result();

    }

    @FunctionalInterface
    public interface BlockingCode<T> {
        public T run() throws Exception;
    }

    /**
     * Use {@link Vertx#executeBlocking(Handler, Handler)} with Futures.
     *
     * @param <T> The type of the result.
     * @param vertx The vertx context.
     * @param blocking The blocking code.
     * @return The future, reporting the result.
     */
    public static <T> Future<T> executeBlocking(final Vertx vertx, final BlockingCode<T> blocking) {
        final Promise<T> result = Promise.promise();

        vertx.executeBlocking(promise -> {

            try {
                promise.complete(blocking.run());
            } catch (Throwable e) {
                promise.fail(e);
            }

        }, result);

        return result.future();
    }
}
