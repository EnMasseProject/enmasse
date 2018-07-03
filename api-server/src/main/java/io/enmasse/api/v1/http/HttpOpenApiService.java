/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/openapi/v2")
public class HttpOpenApiService {

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public InputStream getOpenApiSpec() {
        return getClass().getResourceAsStream("/swagger.json");
    }
}
