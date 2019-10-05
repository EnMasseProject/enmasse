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
import io.enmasse.client.impl.AddressOperationsImpl;
import io.enmasse.client.impl.AddressPlanOperationsImpl;
import io.enmasse.client.impl.AddressSpaceOperationsImpl;
import io.enmasse.client.impl.AddressSpacePlanOperationsImpl;
import io.enmasse.client.impl.AddressSpaceSchemaOperationsImpl;
import io.enmasse.client.impl.AuthenticationServiceOperationsImpl;
import io.enmasse.client.impl.BrokeredInfraConfigOperationsImpl;
import io.enmasse.client.impl.ConsoleServiceOperationsImpl;
import io.enmasse.client.impl.StandardInfraConfigOperationsImpl;
import io.enmasse.client.impl.UserOperationsImpl;
import io.enmasse.user.model.v1.DoneableUser;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserList;
import io.fabric8.kubernetes.client.BaseClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import okhttp3.OkHttpClient;


class EnmasseAPIGroupClient extends BaseClient implements EnmasseOpenShiftAPIGroupDSL {

    public EnmasseAPIGroupClient(KubernetesClient kc, final Config config) throws KubernetesClientException {
        super(kc.adapt(OkHttpClient.class), config);
    }

    @Override
    public MixedOperation<AddressSpace, AddressSpaceList, DoneableAddressSpace, Resource<AddressSpace, DoneableAddressSpace>> addressSpace() {
        return new AddressSpaceOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<Address, AddressList, DoneableAddress, Resource<Address, DoneableAddress>> address() {
        return new AddressOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<AddressSpacePlan, AddressSpacePlanList, DoneableAddressSpacePlan, Resource<AddressSpacePlan, DoneableAddressSpacePlan>> addressSpacePlan() {
        return new AddressSpacePlanOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<AddressPlan, AddressPlanList, DoneableAddressPlan, Resource<AddressPlan, DoneableAddressPlan>> addressPlan() {
        return new AddressPlanOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<BrokeredInfraConfig, BrokeredInfraConfigList, DoneableBrokeredInfraConfig, Resource<BrokeredInfraConfig, DoneableBrokeredInfraConfig>> brokeredInfraConfig() {
        return new BrokeredInfraConfigOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<StandardInfraConfig, StandardInfraConfigList, DoneableStandardInfraConfig, Resource<StandardInfraConfig, DoneableStandardInfraConfig>> standardInfraConfig() {
        return new StandardInfraConfigOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<AuthenticationService, AuthenticationServiceList, DoneableAuthenticationService, Resource<AuthenticationService, DoneableAuthenticationService>> authenticationService() {
        return new AuthenticationServiceOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<AddressSpaceSchema, AddressSpaceSchemaList, DoneableAddressSpaceSchema, Resource<AddressSpaceSchema, DoneableAddressSpaceSchema>> addressSpaceSchema() {
        return new AddressSpaceSchemaOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<User, UserList, DoneableUser, Resource<User, DoneableUser>> messagingUser() {
        return new UserOperationsImpl(httpClient, getConfiguration());
    }

    @Override
    public MixedOperation<ConsoleService, ConsoleServiceList, DoneableConsoleService, Resource<ConsoleService, DoneableConsoleService>> consoleService() {
        return new ConsoleServiceOperationsImpl(httpClient, getConfiguration());
    }
}
