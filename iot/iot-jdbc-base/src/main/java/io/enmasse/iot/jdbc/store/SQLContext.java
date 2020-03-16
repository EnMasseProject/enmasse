/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.sql.SQLConnection;

public class SQLContext<T> {
    private final SQLConnection connection;
    private final T context;

    public SQLContext(final SQLConnection connection) {
        this.connection = connection;
        this.context = null;
    }

    public SQLContext(final SQLConnection connection, final T context) {
        this.connection = connection;
        this.context = context;
    }

    public Future<SQLContext<T>> setAutoCommit(boolean state) {
        final Promise<Void> promise = Promise.promise();
        this.connection.setAutoCommit(state, promise);
        return promise.future().map(this);
    }

    public Future<SQLContext<T>> commit() {
        final Promise<Void> promise = Promise.promise();
        this.connection.commit(promise);
        return promise.future().map(this);
    }

    public Future<SQLContext<T>> rollback() {
        final Promise<Void> promise = Promise.promise();
        this.connection.rollback(promise);
        return promise.future().map(this);
    }

    public Future<SQLContext<T>> commitOrRollback(final AsyncResult<T> result) {
        if (result.succeeded()) {
            return commit();
        } else {
            return rollback();
        }
    }

    public SQLConnection getConnection() {
        return this.connection;
    }

    public T getContext() {
        return this.context;
    }

    public <S> Future<SQLContext<S>> future(final Promise<S> promise) {
        return promise.future()
                .map(v -> new SQLContext<>(this.connection, v));
    }
}
