package io.enmasse.systemtest.selenium;

public interface IWebProperty<T> {
    T get() throws Exception;
}
