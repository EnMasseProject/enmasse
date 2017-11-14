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

import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Set;

@Path("/apis/enmasse.io/v1/addresses")
public class HttpAddressRootService {
    private static final Logger log = LoggerFactory.getLogger(HttpAddressRootService.class.getName());
    private final AddressSpaceApi addressSpaceApi;

    public HttpAddressRootService(AddressSpaceApi addressSpaceApi) {
        this.addressSpaceApi = addressSpaceApi;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAddressSpaces(@Context UriInfo uriInfo) {
        try {
            URI requestUri = uriInfo.getRequestUri();
            List<URI> uriList = new ArrayList<>();
            Set<AddressSpace> addressSpaces = addressSpaceApi.listAddressSpaces();
            for (AddressSpace addressSpace : addressSpaces) {
                uriList.add(requestUri.resolve("/" + addressSpace.getName()));
            }
            return Response.ok(uriList).build();
        } catch (Exception e) {
            log.error("Exception getting address space list", e);
            return Response.serverError().build();
        }
    }
}
