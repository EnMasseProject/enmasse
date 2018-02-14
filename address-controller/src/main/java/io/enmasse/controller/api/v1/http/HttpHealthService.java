/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.v1.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(HttpHealthService.BASE_URI)
public class HttpHealthService {
    public static final String BASE_URI = "/apis/enmasse.io/v1/health";
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getHealth() {
        return Response.status(200).entity(new HealthResponse(200, "OK")).build();
    }

    public class HealthResponse {
        public int status;
        public String message;

        public HealthResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}
