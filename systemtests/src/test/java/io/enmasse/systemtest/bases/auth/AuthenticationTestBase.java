/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.auth;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.bases.TestBase;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.enmasse.systemtest.TestTag.isolated;

@Tag(isolated)
public abstract class AuthenticationTestBase extends TestBase {

    protected static final String anonymousUser = "anonymous";
    protected static final String anonymousPswd = "anonymous";
    protected final List<Address> amqpAddressList = Arrays.asList(
            new AddressBuilder()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("auth-queue")
                    .withPlan(getDefaultPlan(AddressType.QUEUE))
                    .endSpec()
                    .build(),

            new AddressBuilder()
                    .withNewSpec()
                    .withType("topic")
                    .withAddress("auth-topic")
                    .withPlan(getDefaultPlan(AddressType.TOPIC))
                    .endSpec()
                    .build(),

            new AddressBuilder()
                    .withNewSpec()
                    .withType("anycast")
                    .withAddress("auth-anycast")
                    .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                    .endSpec()
                    .build(),

            new AddressBuilder()
                    .withNewSpec()
                    .withType("multicast")
                    .withAddress("auth-multicast")
                    .withPlan(getDefaultPlan(AddressType.MULTICAST))
                    .endSpec()
                    .build());

    @Override
    protected void createAddressSpace(AddressSpace addressSpace) throws Exception {
        super.createAddressSpace(addressSpace);
        List<Address> brokeredAddressList = new ArrayList<>(amqpAddressList);
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString())) {
            brokeredAddressList = amqpAddressList.subList(0, 2);
        }
        brokeredAddressList.forEach(address -> {
            address.getMetadata().setName(addressSpace.getMetadata().getName() + "." + address.getSpec().getAddress());
            address.getMetadata().setNamespace(addressSpace.getMetadata().getNamespace());
        });
        setAddresses(brokeredAddressList.toArray(new Address[0]));
    }

    @Override
    protected void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        super.createAddressSpaceList(addressSpaces);
        List<Address> brokeredAddressList = new ArrayList<>(amqpAddressList);
        for (AddressSpace addressSpace : addressSpaces) {
            if (addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString())) {
                brokeredAddressList = amqpAddressList.subList(0, 2);
            }
            brokeredAddressList.forEach(address -> {
                address.getMetadata().setName(addressSpace.getMetadata().getName() + "." + address.getSpec().getAddress());
                address.getMetadata().setNamespace(addressSpace.getMetadata().getNamespace());
            });
            setAddresses(brokeredAddressList.toArray(new Address[0]));
        }
    }

    protected void testNoneAuthenticationServiceGeneral(AddressSpaceType type, String emptyUser, String emptyPassword) throws Exception {
        String plan = type.equals(AddressSpaceType.STANDARD) ? AddressSpacePlans.STANDARD_SMALL : AddressSpacePlans.BROKERED;
        AddressSpace s3standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(type.toString() + "-s3")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type.toString())
                .withPlan(plan)
                .withNewAuthenticationService()
                .withName("none-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        AddressSpace s4standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(type.toString().toLowerCase() + "-s4")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type.toString())
                .withPlan(plan)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpaceList(s3standard, s4standard);

        assertCanConnect(s3standard, new UserCredentials(emptyUser, emptyPassword), amqpAddressList);
        assertCanConnect(s3standard, new UserCredentials("bob", "pass"), amqpAddressList);

        assertCanConnect(s3standard, new UserCredentials(emptyUser, emptyPassword), amqpAddressList);
        assertCanConnect(s3standard, new UserCredentials("bob", "pass"), amqpAddressList);
        assertCannotConnect(s4standard, new UserCredentials(emptyUser, emptyPassword), amqpAddressList);
        assertCannotConnect(s4standard, new UserCredentials("bob", "pass"), amqpAddressList);
    }

    protected void testStandardAuthenticationServiceGeneral(AddressSpaceType type) throws Exception {
        String plan = type.equals(AddressSpaceType.STANDARD) ? AddressSpacePlans.STANDARD_SMALL : AddressSpacePlans.BROKERED;
        AddressSpace s1brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(type.toString().toLowerCase() + "-s1")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type.toString())
                .withPlan(plan)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        AddressSpace s2brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(type.toString().toLowerCase() + "-s2")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(type.toString().toLowerCase())
                .withPlan(plan)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpaceList(s1brokered, s2brokered);

        // Validate unsuccessful authentication with enmasse authentication service with no credentials
        assertCannotConnect(s1brokered, new UserCredentials(null, null), amqpAddressList);
        assertCannotConnect(s1brokered, new UserCredentials("bob", "s1pass"), amqpAddressList);

        UserCredentials s1Bob = new UserCredentials("bob", "s1pass");
        createOrUpdateUser(s1brokered, s1Bob);

        UserCredentials s1Carol = new UserCredentials("carol", "s2pass");
        createOrUpdateUser(s1brokered, s1Carol);

        assertCannotConnect(s1brokered, new UserCredentials(null, null), amqpAddressList);

        // Validate successful authentication with enmasse authentication service and valid credentials
        assertCanConnect(s1brokered, s1Bob, amqpAddressList);
        assertCanConnect(s1brokered, s1Carol, amqpAddressList);

        // Validate unsuccessful authentication with enmasse authentication service with incorrect credentials
        UserCredentials bobWrongPassword = new UserCredentials(s1Bob.getUsername(), "s2pass");
        assertCannotConnect(s1brokered, bobWrongPassword, amqpAddressList);
        assertCannotConnect(s1brokered, new UserCredentials("alice", "s1pass"), amqpAddressList);


        UserCredentials s2Bob = new UserCredentials("bob", "s2pass");
        createOrUpdateUser(s2brokered, s2Bob);

        //create user with the same credentials in different address spaces
        UserCredentials s2Carol = new UserCredentials("carol", "s2pass");
        createOrUpdateUser(s2brokered, s1Carol);

        assertCanConnect(s1brokered, s1Bob, amqpAddressList);
        assertCanConnect(s1brokered, s2Carol, amqpAddressList);
        assertCanConnect(s2brokered, s2Bob, amqpAddressList);
        assertCanConnect(s2brokered, s2Carol, amqpAddressList);

        assertCannotConnect(s2brokered, s1Bob, amqpAddressList);
        assertCannotConnect(s1brokered, s2Bob, amqpAddressList);
    }

}
