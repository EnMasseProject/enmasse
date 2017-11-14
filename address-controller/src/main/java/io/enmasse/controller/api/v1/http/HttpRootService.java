/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.controller.api.v1.http;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/")
public class HttpRootService {
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAll(@Context UriInfo uriInfo) {
        List<URI> uriList = new ArrayList<>();
        URI baseUri = uriInfo.getBaseUri();
        uriList.add(baseUri.resolve("/apis/enmasse.io/v1"));
        uriList.add(baseUri.resolve("/osbapi"));
        return Response.status(200).entity(uriList).build();
    }
}
