/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import static io.enmasse.iot.utils.MoreThrowables.causeOf;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.hono.tracing.TracingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.sql.SQLConnection;

public final class SQL {

    private static final Logger log = LoggerFactory.getLogger(SQL.class);

    private SQL() {}

    public static <T> Future<T> translateException(final Throwable e) {

        var sqlError = causeOf(e, SQLException.class).orElse(null);

        if (sqlError == null) {
            return Future.failedFuture(e);
        }

        log.debug("SQL Error: {}", sqlError.getSQLState());
        final String code = sqlError.getSQLState();
        if (code == null) {
            return Future.failedFuture(e);
        }

        switch (code) {
            case "23000": //$FALL-THROUGH$
            case "23505":
                return Future.failedFuture(new DuplicateKeyException(e));
        }

        return Future.failedFuture(e);
    }

    public static Future<SQLConnection> setAutoCommit(final Tracer tracer, final SpanContext context, final SQLConnection connection, boolean state) {
        final Span span = startSqlSpan(tracer, context, "set autocommit", builder -> {
            builder.withTag("db.autocommit", state);
        });
        final Promise<Void> promise = Promise.promise();
        connection.setAutoCommit(state, promise);
        return finishSpan(promise.future().map(connection), span, null);
    }

    public static Future<SQLConnection> commit(final Tracer tracer, final SpanContext context, final SQLConnection connection) {
        final Span span = startSqlSpan(tracer, context, "commit", null);
        final Promise<Void> promise = Promise.promise();
        connection.commit(promise);
        return finishSpan(promise.future().map(connection), span, null);
    }

    public static Future<SQLConnection> rollback(final Tracer tracer, final SpanContext context, final SQLConnection connection) {
        final Span span = startSqlSpan(tracer, context, "rollback", null);
        final Promise<Void> promise = Promise.promise();
        connection.rollback(promise);
        return finishSpan(promise.future().map(connection), span, null);
    }

    public static Span startSqlSpan(final Tracer tracer, final SpanContext context, final String operationName, final Consumer<Tracer.SpanBuilder> customizer) {

        if (tracer == null || context == null) {
            return null;
        }

        final SpanBuilder builder = TracingHelper
                .buildChildSpan(tracer, context, operationName, SQL.class.getSimpleName())
                .withTag(Tags.DB_TYPE.getKey(), "sql");

        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.start();

    }

    public static <T> Future<T> finishSpan(final Future<T> future, final Span span, final BiConsumer<T, Map<String, Object>> extractor) {
        if (span == null) {
            return future;
        }

        return future
                .map(r -> traceSuccess(r, span, extractor))
                .recover(e -> traceError(e, span));
    }

    private static <T> T traceSuccess(final T result, final Span span, final BiConsumer<T, Map<String, Object>> extractor) {
        final Map<String, Object> log = new HashMap<>();
        log.put(Fields.EVENT, "success");
        if (extractor != null) {
            extractor.accept(result, log);
        }
        span.log(log);
        span.finish();
        return result;
    }

    private static <T> Future<T> traceError(Throwable e, Span span) {
        span.log(Map.of(
                Fields.EVENT, "error",
                Fields.ERROR_KIND, e.getClass().getName(),
                Fields.ERROR_OBJECT, e,
                Fields.MESSAGE, e.getMessage(),
                Fields.STACK, Throwables.getStackTraceAsString(e)));
        Tags.ERROR.set(span, true);
        span.finish();
        return Future.failedFuture(e);
    }

    public static String getDatabaseDialect(final String url) {
        final URI uri = URI.create(url);
        final String scheme = uri.getScheme();
        if (!"jdbc".equals(scheme)) {
            throw new IllegalArgumentException("URL is not a JDBC url: " + url);
        }
        final URI subUri = URI.create(uri.getSchemeSpecificPart());
        return subUri.getScheme();
    }
}
