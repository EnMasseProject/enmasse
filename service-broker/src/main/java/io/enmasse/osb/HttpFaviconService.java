/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/favicon.ico")
@Produces("image/x-icon")
public class HttpFaviconService {
    private static final String imageFile = "favicon.ico";

    @GET
    public Response getFavicon() {
        return Response.ok(getClass().getResourceAsStream(imageFile)).build();
    }
}
