/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.router.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouterConfigTest {
    @Test
    public void testSerialize() throws IOException {
        RouterConfig config = new RouterConfig(
                new Router(),
                Collections.singletonList(new SslProfile()),
                Collections.singletonList(new AuthServicePlugin()),
                Collections.singletonList(new Listener()),
                Collections.singletonList(new Policy()),
                Collections.singletonList(new Connector()),
                Collections.singletonList(new LinkRoute()),
                Collections.singletonList(new Address()),
                Collections.singletonList(new VhostPolicy()));


        byte[] serialized = config.asJson();
        System.out.println(new String(serialized, StandardCharsets.UTF_8));
        RouterConfig deser = RouterConfig.fromJson(serialized);

        assertEquals(deser.getRouter(), config.getRouter());
        assertEquals(deser.getAddresses(), config.getAddresses());
        assertEquals(deser.getAuthServicePlugins(), config.getAuthServicePlugins());
        assertEquals(deser.getConnectors(), config.getConnectors());
        assertEquals(deser.getLinkRoutes(), config.getLinkRoutes());
        assertEquals(deser.getListeners(), config.getListeners());
        assertEquals(deser.getPolicies(), config.getPolicies());
        assertEquals(deser.getVhosts(), config.getVhosts());

        assertEquals(deser, config);
    }
}
