/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.enmasse.systemtest.resources.CliOutputData.CliOutputDataType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CliOutputDataTest {

    @Test
    public void testAddressSpace() {
        var cli = new CliOutputData(
                "NAME   TYPE       PLAN                 READY   PHASE         STATUS                                                                                                                      AGE\n" +
                "iot    standard   standard-unlimited   true    Active                                                                                                                                    2d15h\n" +
                "iot3   standard   standard-unlimited   false   Configuring   [The following deployments are not ready: [admin.c5d5468] The following stateful sets are not ready: [qdrouterd-c5d5468]]   7s\n" +
                "",
                CliOutputDataType.ADDRESS_SPACE);
        assertThat(cli.getData(), is(Arrays.asList(
                new CliOutputData.AddressSpaceRow(new String[] {"iot", "standard", "standard-unlimited", "true", "Active", "", "2d15h", }),
                new CliOutputData.AddressSpaceRow(new String[] {"iot3", "standard", "standard-unlimited", "false", "Configuring", "[The following deployments are not ready: [admin.c5d5468] The following stateful sets are not ready: [qdrouterd-c5d5468]]", "7s", })
                )));
    }

    @Test
    public void testAddress () {
        var cli = new CliOutputData(
                "NAME                                                               ADDRESS                        TYPE      PLAN                     READY   PHASE    STATUS   AGE\n" +
                "iot.commandece2019iot-cfd84132-ca7a-3633-8e01-a1f31b681d3f         command/ece2019.iot            anycast   standard-small-anycast   true    Active            18h\n" +
                "iot.commandresponseece2019i-1c5fc2a8-66f7-3c2e-b784-221bc9adf156   command_response/ece2019.iot   anycast   standard-small-anycast   true    Active            18h\n" +
                "iot.controlece2019iot-4f044993-5d44-3595-a1e8-16d2aa165431         control/ece2019.iot            anycast   standard-small-anycast   true    Active            18h\n" +
                "iot.eventece2019iot-e95e65a9-9717-3008-a429-205c9c76a789           event/ece2019.iot              queue     standard-small-queue     true    Active            18h\n" +
                "iot.telemetryece2019iot-19517735-7115-3b8e-8751-b8c44aee0f1c       telemetry/ece2019.iot          anycast   standard-small-anycast   true    Active            18h\n" +
                "", CliOutputDataType.ADDRESS);
        assertThat(cli.getData(), is(Arrays.asList(
                new CliOutputData.AddressRow(new String[] {"iot.commandece2019iot-cfd84132-ca7a-3633-8e01-a1f31b681d3f", "command/ece2019.iot", "anycast", "standard-small-anycast", "true", "Active", "", "18h", }),
                new CliOutputData.AddressRow(new String[] {"iot.commandresponseece2019i-1c5fc2a8-66f7-3c2e-b784-221bc9adf156", "command_response/ece2019.iot", "anycast", "standard-small-anycast", "true", "Active", "", "18h", }),
                new CliOutputData.AddressRow(new String[] {"iot.controlece2019iot-4f044993-5d44-3595-a1e8-16d2aa165431", "control/ece2019.iot", "anycast", "standard-small-anycast", "true", "Active", "", "18h", }),
                new CliOutputData.AddressRow(new String[] {"iot.eventece2019iot-e95e65a9-9717-3008-a429-205c9c76a789", "event/ece2019.iot", "queue", "standard-small-queue", "true", "Active", "", "18h", }),
                new CliOutputData.AddressRow(new String[] {"iot.telemetryece2019iot-19517735-7115-3b8e-8751-b8c44aee0f1c", "telemetry/ece2019.iot", "anycast", "standard-small-anycast", "true", "Active", "", "18h", })
                )));
    }

    @Test
    public void testMessagingUser () {
        var cli = new CliOutputData(
                "NAME                                               USERNAME                                       TYPE       PHASE    AGE\n" +
                "iot.adapter-074334ae-3d1f-11ea-a7b2-00163e294db3   adapter-074334ae-3d1f-11ea-a7b2-00163e294db3   password   Active   18h\n" +
                "iot.consumer                                       consumer                                       password   Active   18h\n" +
                "", CliOutputDataType.USER);
        assertThat(cli.getData(), is(Arrays.asList(
                new CliOutputData.UserRow(new String[] {"iot.adapter-074334ae-3d1f-11ea-a7b2-00163e294db3", "adapter-074334ae-3d1f-11ea-a7b2-00163e294db3", "password", "Active", "18h", }),
                new CliOutputData.UserRow(new String[] {"iot.consumer", "consumer", "password", "Active", "18h", })
                )));
    }

}
