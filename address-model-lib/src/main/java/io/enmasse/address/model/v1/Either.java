/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

public class Either<L, R> {
    private final L l;
    private final R r;

    private Either(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public static <L, R> Either createLeft(L l) {
        return new Either<>(l, null);
    }

    public static <L, R> Either createRight(R r) {
        return new Either<>(null, r);
    }

    public boolean isLeft() {
        return l != null;
    }

    public boolean isRight() {
        return r != null;
    }

    public L getLeft() {
        return l;
    }

    public R getRight() {
        return r;
    }
}
