/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.common.api.model;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;

@JacksonAnnotationsInside
@JsonPropertyOrder({ "apiVersion", "kind", "metadata" })
@JsonAppend(prepend = true, props = {
                        @Prop(name = "apiVersion", value = ApiVersionWriter.class, type = String.class, required = true),
                        @Prop(name = "kind", value = KindWriter.class, type = String.class, required = true)
})
@Retention(RUNTIME)
@Documented
@Target(ElementType.TYPE)
public @interface ApiVersion {

    String value();
}