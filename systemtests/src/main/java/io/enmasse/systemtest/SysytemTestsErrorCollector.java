/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import org.hamcrest.Matcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;


public class SysytemTestsErrorCollector {
    private List<Exception> collector = new ArrayList<>();

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public void clear() {
        this.collector.clear();
    }

    public void addError(Exception ex) {
        this.collector.add(ex);
    }

    public <T> void checkThat(T value, Matcher<T> matcher) {
        try {
            assertThat(value, matcher);
        } catch (Exception ex) {
            this.collector.add(ex);
        }
    }

    public boolean verify() {
        return this.collector.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.collector.forEach(exception ->
                builder.append(exception.getMessage())
                        .append(System.getProperty("line.separator"))
                        .append(getStackTrace(exception))
                        .append(System.getProperty("line.separator")));
        return builder.toString();
    }
}
