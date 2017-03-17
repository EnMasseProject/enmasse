/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.address.controller.api.v1.http;

import enmasse.address.controller.admin.AddressManagerFactory;
import enmasse.address.controller.api.v1v2common.RestServiceBase;
import enmasse.address.controller.api.v1v2common.common.AddressProperties;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.InstanceId;
import io.vertx.core.Vertx;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v1/enmasse/addresses")
public class RestServiceV1 extends RestServiceBase {

    public RestServiceV1(@Context InstanceId instanceId, @Context AddressManagerFactory addressManagerFactory, @Context Vertx vertx) {
        super(instanceId, addressManagerFactory, vertx);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public void getAddresses(@Suspended final AsyncResponse response) {
        super.getAddresses(response);
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void putAddresses(Map<String, AddressProperties> addressMap, @Suspended final AsyncResponse response) {
        super.putAddresses(mapToDestinations(addressMap), response);
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void deleteAddresses(List<String> data, @Suspended final AsyncResponse response) {
        super.deleteAddresses(data, response);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void appendAddresses(Map<String, AddressProperties> addressMap, @Suspended final AsyncResponse response) {
        super.appendAddresses(mapToDestinations(addressMap), response);
    }

    private static Set<DestinationGroup> mapToDestinations(Map<String, AddressProperties> addressMap) {
        return addressMap.entrySet().stream()
                .map(e -> {
                    DestinationGroup.Builder groupBuilder = new DestinationGroup.Builder(e.getKey());
                    groupBuilder.destination(new Destination(e.getKey(), e.getKey(), e.getValue().store_and_forward, e.getValue().multicast, Optional.ofNullable(e.getValue().flavor), Optional.empty()));
                    return groupBuilder.build();
                })
                .collect(Collectors.toSet());
    }

    protected Map<String, AddressProperties> getResponseEntity(Collection<DestinationGroup> destinationGroups) {
        Map<String, AddressProperties> map = new LinkedHashMap<>();
        for (DestinationGroup destinationGroup : destinationGroups) {
            for (Destination destination : destinationGroup.getDestinations()) {
                String flavor = destination.flavor().orElse(null);
                map.put(destination.address(), new AddressProperties(destination.storeAndForward(), destination.multicast(), flavor));
            }
        }
        return map;
    }
}
