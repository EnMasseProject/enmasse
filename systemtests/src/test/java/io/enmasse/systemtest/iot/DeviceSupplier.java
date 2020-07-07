/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.iot.IoTTestSession.Device;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;

/**
 * A supplier for devices. Required due to an issue in Surefire.
 * <p>
 * This is a workaround for SUREFIRE-1799, which silently drops unit tests in case
 * there is an exception being thrown in a method of a {@link MethodSource}. As we
 * create {@link Device}s for parametrized tests and creation of devices might fail,
 * that will be an issue.
 * <p>
 * The idea is to delay the actual execution of the device creation until it
 * is running in the actual test case. So instead of creating devices, we create
 * code that creates devices. The call to the {@link #get()} method should perform
 * the actual device creation, and it may fail, which is reported as a test failure.
 * <p>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1799">SUREFIRE-1799</a>
 */
@FunctionalInterface
public interface DeviceSupplier {

    public Device get() throws Exception;

    /**
     * Allows to override the output of the {@link #toString()} method.
     * <p>
     * This may be used to provide a stable name for parameterized test.
     *
     * @implNote This was originally part of the {@link Device} class. Due to
     * SUREFIRE-1799 this code currently lives here.
     *
     * @param name The value to report from {@link #toString()}.
     * @return The new instance, reporting the provided name.
     */
    static DeviceSupplier named(final String name, final DeviceSupplier supplier) {

        Objects.requireNonNull(name);
        Objects.requireNonNull(supplier);

        return new DeviceSupplier() {

            @Override
            public Device get() throws Exception {
                return supplier.get();
            }

            @Override
            public String toString() {
                return name;
            }
        };

    }

}
