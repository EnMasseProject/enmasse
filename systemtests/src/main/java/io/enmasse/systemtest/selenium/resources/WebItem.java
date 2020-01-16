/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.selenium.resources;

import org.eclipse.hono.util.Strings;
import org.openqa.selenium.WebElement;

public class WebItem {
    protected WebElement webItem;

    protected int defaultInt(String value) {
        return Strings.isNullOrEmpty(value) ? 0 : Integer.parseInt(value);

    }

    protected double defaultDouble(String value) {
        return Strings.isNullOrEmpty(value) ? 0.0 : Double.parseDouble(value);
    }
}
