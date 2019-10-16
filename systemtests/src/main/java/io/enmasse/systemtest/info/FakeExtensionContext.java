/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.info;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;

public class FakeExtensionContext implements ExtensionContext{

    private Optional<Method> method;

    public FakeExtensionContext(Optional<Method> method) throws ClassNotFoundException {
        this.method = method;
    }

    @Override
    public Optional<ExtensionContext> getParent() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public ExtensionContext getRoot() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getUniqueId() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Set<String> getTags() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<AnnotatedElement> getElement() {
        return this.method.map(m -> (AnnotatedElement)m);
    }

    @Override
    public Optional<Class<?>> getTestClass() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<Lifecycle> getTestInstanceLifecycle() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<Object> getTestInstance() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<TestInstances> getTestInstances() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<Method> getTestMethod() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<Throwable> getExecutionException() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Optional<String> getConfigurationParameter(String key) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void publishReportEntry(Map<String, String> map) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Store getStore(Namespace namespace) {
        throw new UnsupportedOperationException("not implemented");
    }

}