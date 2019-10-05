/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.AddressSpaceSchema;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.address.model.DoneableAddress;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.address.model.DoneableAddressSpaceSchema;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanList;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanList;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceList;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigList;
import io.enmasse.admin.model.v1.ConsoleService;
import io.enmasse.admin.model.v1.ConsoleServiceList;
import io.enmasse.admin.model.v1.DoneableAddressPlan;
import io.enmasse.admin.model.v1.DoneableAddressSpacePlan;
import io.enmasse.admin.model.v1.DoneableAuthenticationService;
import io.enmasse.admin.model.v1.DoneableBrokeredInfraConfig;
import io.enmasse.admin.model.v1.DoneableConsoleService;
import io.enmasse.admin.model.v1.DoneableStandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigList;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public interface EnmasseKubernetesAPIGroupDSL extends Client {

    MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> addressSpace();
    MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> address();
    MixedOperation<AddressSpacePlan, AddressSpacePlanList, DoneableAddressSpacePlan, Resource<AddressSpacePlan, DoneableAddressSpacePlan>> addressSpacePlan();
    MixedOperation<AddressPlan, AddressPlanList, DoneableAddressPlan, Resource<AddressPlan, DoneableAddressPlan>> addressPlan();
    MixedOperation<BrokeredInfraConfig, BrokeredInfraConfigList, DoneableBrokeredInfraConfig, Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig>> brokeredInfraConfig();
    MixedOperation<StandardInfraConfig, StandardInfraConfigList, DoneableStandardInfraConfig, Resource<StandardInfraConfig, DoneableStandardInfraConfig>> standardInfraConfig();
    MixedOperation<AuthenticationService, AuthenticationServiceList, DoneableAuthenticationService, Resource<AuthenticationService, DoneableAuthenticationService>> authenticationService();
    MixedOperation<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema, Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>> addressSpaceSchema();
    MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> messagingUser();
    MixedOperation<ConsoleService, ConsoleServiceList, DoneableConsoleService, Resource<ConsoleService, DoneableConsoleService>> consoleService();
}
