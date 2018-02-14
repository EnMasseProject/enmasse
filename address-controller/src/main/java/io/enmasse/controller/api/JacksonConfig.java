/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.v1.CodecV1;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Providing custom encoder
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    public ObjectMapper getContext(Class<?> objectType) {
        return CodecV1.getMapper();
    }

}
