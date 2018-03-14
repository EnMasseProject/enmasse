/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import java.util.List;

public interface Store<T> {
    void add(T t) throws InterruptedException;
    void update(T t) throws InterruptedException;
    void delete(T t) throws InterruptedException;
    List<T> list();
    List<String> listKeys();
    void replace(List<T> list, String resourceVersion) throws InterruptedException;
}
