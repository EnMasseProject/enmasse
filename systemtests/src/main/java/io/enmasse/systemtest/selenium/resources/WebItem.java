/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import org.openqa.selenium.WebElement;

import java.util.Optional;
import java.util.function.Predicate;

public class WebItem {
    protected WebElement webItem;

    protected int defaultInt(String value) {
        return Optional.ofNullable(value)
                .filter(Predicate.not(String::isEmpty))
                .map(Integer::parseInt)
                .orElse(0);
    }

    protected double defaultDouble(String value) {
        return Optional.ofNullable(value)
                .filter(Predicate.not(String::isEmpty))
                .map(Double::parseDouble)
                .orElse(0.0);
    }
}
