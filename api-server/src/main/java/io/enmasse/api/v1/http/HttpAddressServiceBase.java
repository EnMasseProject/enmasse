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
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.api.common.Status;
import io.enmasse.api.v1.AddressApiHelper;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.PartialObjectMetadata;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.k8s.model.v1beta1.TableColumnDefinition;
import io.enmasse.k8s.model.v1beta1.TableRow;
import io.enmasse.k8s.util.TimeUtil;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * HTTP API for operating on addresses within an address space
 */
public class HttpAddressServiceBase {
    private static final Logger log = LoggerFactory.getLogger(HttpAddressServiceBase.class.getName());
    private final AddressApiHelper apiHelper;
    private final Clock clock;

    public HttpAddressServiceBase(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider, Clock clock) {
        this.clock = clock;
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
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(namespace, verb, "addresses", "enmasse.io"))) {
            throw Exceptions.notAuthorizedException();
        }
    }

    Response getAddressList(SecurityContext securityContext, String acceptHeader, String namespace, String addressSpace, String address, String labelSelector) throws Exception {

        return doRequest("Error listing addresses",() -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            if (address == null) {
                if (labelSelector != null) {
                    Map<String, String> labels = AddressApiHelper.parseLabelSelector(labelSelector);
                    AddressList list = apiHelper.getAddressesWithLabels(namespace, addressSpace, labels);
                    return Response.ok(formatResponse(acceptHeader, list)).build();
                } else {
                    AddressList list = apiHelper.getAddresses(namespace, addressSpace);
                    return Response.ok(formatResponse(acceptHeader, list)).build();
                }
            } else {
                AddressList list = apiHelper.getAddresses(namespace, addressSpace);
                for (Address entity : list) {
                    if (entity.getAddress().equals(address)) {
                        return Response.ok(formatResponse(acceptHeader, entity)).build();
                    }
                }
                return Response.status(404).entity(Status.notFound("Address", address)).build();
            }
        });
    }

    Response getAddress(SecurityContext securityContext, String acceptHeader, String namespace, String addressSpace, String address) throws Exception {
        return doRequest("Error getting address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.list);
            return apiHelper.getAddress(namespace, addressSpace, address)
                    .map(a -> Response.ok(formatResponse(acceptHeader, a)).build())
                    .orElseGet(() -> Response.status(404).entity(Status.notFound("Address", address)).build());
        });
    }

    Response createAddress(SecurityContext securityContext, UriInfo uriInfo, String namespace, String addressSpace, Either<Address, AddressList> payload) throws Exception {
        if (payload.isLeft()) {
            return createAddress(securityContext, uriInfo, namespace, addressSpace, payload.getLeft());
        } else {
            return createAddresses(securityContext, uriInfo, namespace, addressSpace, payload.getRight());
        }
    }

    private Response createAddress(SecurityContext securityContext, UriInfo uriInfo, String namespace, String addressSpace, Address address) throws Exception {
        checkRequestBodyNotNull(address);
        Address finalAddress = setAddressDefaults(namespace, addressSpace, address);
        return doRequest("Error creating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            Address created = apiHelper.createAddress(addressSpace, finalAddress);
            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(created.getName());
            return Response.created(builder.build()).entity(created).build();
        });
    }

    private Response createAddresses(SecurityContext securityContext, UriInfo uriInfo, String namespace, String addressSpace, AddressList addressList) throws Exception {
        checkRequestBodyNotNull(addressList);
        return doRequest("Error creating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.create);
            Set<Address> finalAddresses = addressList.stream()
                    .map(a -> setAddressDefaults(namespace, addressSpace, a))
                    .collect(Collectors.toSet());
            apiHelper.createAddresses(addressSpace, finalAddresses);
            return Response.created(uriInfo.getAbsolutePathBuilder().build()).build();
        });
    }

    private static Address setAddressDefaults(String namespace, String addressSpace, Address address) {
        if (address.getNamespace() == null || address.getAddressSpace() == null || address.getName() == null) {
            Address.Builder builder = new Address.Builder(address);
            if (address.getNamespace() == null) {
                builder.setNamespace(namespace);
            }

            if (address.getAddressSpace() == null) {
                builder.setAddressSpace(addressSpace);
            }

            if (address.getName() == null) {
                builder.setName(Address.generateName(addressSpace, address.getAddress()));
            }

            address = builder.build();
        }

        if (address.getLabel(LabelKeys.ADDRESS_TYPE) == null) {
            address.putLabel(LabelKeys.ADDRESS_TYPE, address.getType());
        }

        return address;
    }

    private void checkRequestBodyNotNull(Object object) {
        if (object == null) {
            throw new BadRequestException("Missing request body");
        }
    }

    Response replaceAddress(SecurityContext securityContext, String namespace, String addressSpace, String addressNameFromURL, Address payload) throws Exception {
        checkRequestBodyNotNull(payload);
        checkAddressObjectNameNotNull(payload, addressNameFromURL);
        checkMatchingAddressName(addressNameFromURL, payload);
        Address finalAddress = setAddressDefaults(namespace, addressSpace, payload);
        return doRequest("Error updating address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.update);
            try {
                Address replaced = apiHelper.replaceAddress(addressSpace, finalAddress);
                return Response.ok(replaced).build();
            } catch (NotFoundException e) {
                return Response.status(404).entity(Status.notFound("Address", finalAddress.getName())).build();
            }
        });
    }

    protected void checkAddressObjectNameNotNull(Address address, String addressNameFromURL) {
        if (address.getName() == null) {
            throw new BadRequestException("the name of the object (" + addressNameFromURL + " based on URL) was undeterminable: name must be provided");
        }
    }

    private void checkMatchingAddressName(String addressNameFromURL, Address addressFromPayload) {
        if (addressFromPayload.getName() != null && !addressFromPayload.getName().equals(addressNameFromURL)) {
            throw new BadRequestException("the name of the object (" + addressFromPayload.getName() + ") does not match the name on the URL (" + addressNameFromURL + ")");
        }
    }

    Response deleteAddress(SecurityContext securityContext, String namespace, String addressSpace, String addressName) throws Exception {
        return doRequest("Error deleting address", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            Address address = apiHelper.deleteAddress(namespace, addressSpace, addressName);
            if (address == null) {
                return Response.status(404).entity(Status.notFound("Address", addressName)).build();
            }
            return Response.ok(Status.successStatus(200, "Address", addressName, address.getUid())).build();
        });
    }

    protected Response deleteAddresses(SecurityContext securityContext, String namespace) throws Exception {
        return doRequest("Error deleting addresses", () -> {
            verifyAuthorized(securityContext, namespace, ResourceVerb.delete);
            apiHelper.deleteAddresses(namespace);
            return Response.ok(Status.successStatus(200)).build();
        });
    }

    private Object formatResponse(String acceptHeader, Address address) {
        if (isTableFormat(acceptHeader)) {
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(Collections.singletonList(address)));
        } else {
            return address;
        }
    }

    private Object formatResponse(String acceptHeader, AddressList list) {
        if (isTableFormat(acceptHeader)) {
            return new Table(new ListMeta(), tableColumnDefinitions, createRows(list));
        } else {
            return list;
        }
    }

    private boolean isTableFormat(String acceptHeader) {
        return acceptHeader != null && acceptHeader.contains("as=Table") && acceptHeader.contains("g=meta.k8s.io") && acceptHeader.contains("v=v1beta1");
    }

    private static final List<TableColumnDefinition> tableColumnDefinitions = Arrays.asList(
            new TableColumnDefinition("Name must be unique within an address space and namespace.",
                    "name",
                    "Name",
                    0,
                    "string"),
            new TableColumnDefinition("Address of the address resource.",
                    "",
                    "Address",
                    0,
                    "string"),
            new TableColumnDefinition("Address space of the address resource.",
                    "",
                    "AddressSpace",
                    1,
                    "string"),
            new TableColumnDefinition("Type of address",
                    "",
                    "Type",
                    1,
                    "string"),
            new TableColumnDefinition("Plan of address resource",
                    "",
                    "Plan",
                    1,
                    "string"),
            new TableColumnDefinition("The readiness of this address.",
                    "",
                    "Ready",
                    0,
                    "string"),
            new TableColumnDefinition("The phase of this address.",
                    "",
                    "Phase",
                    0,
                    "string"),
            new TableColumnDefinition("The timestamp representing server time when this address was created.",
                    "",
                    "Age",
                    0,
                    "string"),
            new TableColumnDefinition("The status messages reported for the address.",
                    "",
                    "Status",
                    1,
                    "string"));

    private List<TableRow> createRows(List<Address> addressList) {
        Instant now = clock.instant();
        return addressList.stream()
                .map(address -> new TableRow(
                        Arrays.asList(
                                address.getName(),
                                address.getAddress(),
                                address.getAddressSpace(),
                                address.getType(),
                                address.getPlan(),
                                address.getStatus().isReady(),
                                address.getStatus().getPhase(),
                                Optional.ofNullable(address.getCreationTimestamp())
                                        .map(s -> TimeUtil.formatHumanReadable(Duration.between(TimeUtil.parseRfc3339(s), now)))
                                        .orElse(""),
                                String.join(". ", address.getStatus().getMessages())),
                        new PartialObjectMetadata(new ObjectMetaBuilder()
                                .withNamespace(address.getNamespace())
                                .withName(address.getName())
                                .withLabels(address.getLabels())
                                .withAnnotations(address.getAnnotations())
                                .withCreationTimestamp(address.getCreationTimestamp())
                                .withSelfLink(address.getSelfLink())
                                .withUid(address.getUid())
                                .withResourceVersion(address.getResourceVersion())
                                .build())))
                .collect(Collectors.toList());
    }
}
