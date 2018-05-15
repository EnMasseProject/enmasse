/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.v1.Either;
import io.enmasse.api.auth.RbacSecurityContext;
import io.enmasse.api.auth.ResourceVerb;
import io.enmasse.api.common.Exceptions;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * HTTP API for operating on addresses within an address space
 */
public class HttpAddressServiceBase {
    private static final Logger log = LoggerFactory.getLogger(HttpAddressServiceBase.class.getName());
    private final AddressApiHelper apiHelper;

    public HttpAddressServiceBase(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider) {
        this.apiHelper = new AddressApiHelper(addressSpaceApi, schemaProvider);
    }

    private Response doRequest(String errorMessage, Callable<Response> request) throws Exception {
        try {
            return request.call();
        } catch (Exception e) {
            log.error(errorMessage, e);
            throw e;
        }
    }

    private static void verifyAuthorized(SecurityContext securityContext, String namespace, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "addresses"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    Response getAddressList(SecurityContext securityContext, String namespace, String addressSpace, String address, String labelSelector) throws Exception {
        return doRequest("Error listing addresses",() -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            if (address == null) {
                if (labelSelector != null) {
                    Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                    AddressList list = apiHelper.getAddressesWithLabels(namespace, addressSpace, labels);
                    return Response.ok(list).build();
                } else {
                    AddressList list = apiHelper.getAddresses(namespace, addressSpace);
                    return Response.ok(list).build();
                }
            } else {
                AddressList list = apiHelper.getAddresses(namespace, addressSpace);
                for (Address entity : list) {
                    if (entity.getAddress().equals(address)) {
                        return Response.ok(entity).build();
                    }
                }
                throw new NotFoundException("Address " + address + " not found");
            }
        });
    }

    Response getAddress(SecurityContext securityContext, String namespace, String addressSpace, String address) throws Exception {
        return doRequest("Error getting address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            return Response.ok(apiHelper.getAddress(namespace, addressSpace, address)
                    .orElseThrow(() -> Exceptions.notFoundException("Address " + address + " not found")))
                    .build();
        });
    }

    Response createAddress(SecurityContext securityContext, UriInfo uriInfo, String namespace, String addressSpace, Either<Address, AddressList> payload) throws Exception {
        if (payload.isLeft()) {
            return createAddress(securityContext, uriInfo, namespace, addressSpace, payload.getLeft());
        } else {
            for (Address address : payload.getRight()) {
                createAddress(securityContext, uriInfo, namespace, addressSpace, address);
            }
            return Response.created(uriInfo.getAbsolutePathBuilder().build()).build();
        }
    }

    private Response createAddress(SecurityContext securityContext, UriInfo uriInfo, String namespace, String addressSpace, Address address) throws Exception {
        checkNotNull(address);
        Address finalAddress = setAddressDefaults(namespace, addressSpace, address);
        return doRequest("Error creating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            Address created = apiHelper.createAddress(addressSpace, finalAddress);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }

    static Address setAddressDefaults(String namespace, String addressSpace, Address address) {
        if (address.getNamespace() == null || address.getAddressSpace() == null) {
            Address.Builder builder = new Address.Builder(address);
            if (address.getNamespace() == null) {
                builder.setNamespace(namespace);
            }

            if (address.getAddressSpace() == null) {
                builder.setAddressSpace(addressSpace);
            }
            address = builder.build();
        }

        if (address.getLabel(LabelKeys.ADDRESS_TYPE) == null) {
            address.putLabel(LabelKeys.ADDRESS_TYPE, address.getType());
        }

        return address;
    }

    private void checkNotNull(Object object) {
        if (object == null) {
            throw new BadRequestException("Missing request body");
        }
    }

    Response replaceAddresses(SecurityContext securityContext, String namespace, String addressSpace, Address address) throws Exception {
        checkNotNull(address);
        Address finalAddress = setAddressDefaults(namespace, addressSpace, address);
        return doRequest("Error updating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            Address replaced = apiHelper.replaceAddress(addressSpace, finalAddress);
            return Response.ok(replaced).build();
        });
    }

    Response deleteAddress(SecurityContext securityContext, String namespace, String addressSpace, String addressName) throws Exception {
        return doRequest("Error deleting address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            apiHelper.deleteAddress(namespace, addressSpace, addressName);
            return Response.ok().build();
        });
    }
}
