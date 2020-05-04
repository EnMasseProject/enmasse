/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingTenant;
import io.enmasse.systemtest.annotations.DefaultMessagingInfra;
import io.enmasse.systemtest.annotations.DefaultMessagingTenant;
import io.enmasse.systemtest.annotations.SkipResourceLogging;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedSharedInfra;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.scale.ResultWriter;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ISOLATED_SHARED_INFRA;
import static io.enmasse.systemtest.TestTag.SCALE;

@Tag(ISOLATED_SHARED_INFRA)
@Tag(SCALE)
@SkipResourceLogging
public class MessagingAddressPerfTest extends TestBase implements ITestIsolatedSharedInfra {
    /**
     * Simple performance test to be able to track create and delete performance of addresses.
     */
    @Test
    @DefaultMessagingInfra
    @DefaultMessagingTenant
    public void testCreateDelete() throws Exception {
        MessagingTenant tenant = infraResourceManager.getDefaultMessagingTenant();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(tenant.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .addToProtocols("AMQP")
                .editOrNewNodePort()
                .endNodePort()
                .endSpec()
                .build();
        infraResourceManager.createResource(endpoint);

        int numAnycast = 1000;
        int numQueues = 300;
        List<MessagingAddress> addresses = new ArrayList<>();

        for (int i = 0; i < numAnycast; i++) {
            addresses.add(new MessagingAddressBuilder()
                    .editOrNewMetadata()
                    .withNamespace(tenant.getMetadata().getNamespace())
                    .withName(String.format("anycast%d", i))
                    .endMetadata()
                    .editOrNewSpec()
                    .editOrNewAnycast()
                    .endAnycast()
                    .endSpec()
                    .build());
        }

        for (int i = 0; i < numQueues; i++) {
            addresses.add(new MessagingAddressBuilder()
                    .editOrNewMetadata()
                    .withNamespace(tenant.getMetadata().getNamespace())
                    .withName(String.format("queue%d", i))
                    .endMetadata()
                    .editOrNewSpec()
                    .editOrNewQueue()
                    .endQueue()
                    .endSpec()
                    .build());
        }

        MessagingAddress[] toCreate = addresses.toArray(new MessagingAddress[0]);

        long start = System.nanoTime();
        infraResourceManager.createResource(true, toCreate);
        long end = System.nanoTime();

        long startDelete = System.nanoTime();
        infraResourceManager.deleteResource(toCreate);
        for (MessagingAddress next : addresses) {
            while (MessagingAddressResourceType.getOperation().inNamespace(next.getMetadata().getNamespace()).withName(next.getMetadata().getName()).get() != null) {
                Thread.sleep(1000);
            }
        }
        long endDelete = System.nanoTime();

        Map<String, Object> results = new HashMap<>();
        results.put("create", TimeUnit.NANOSECONDS.toSeconds(end - start));
        results.put("delete", TimeUnit.NANOSECONDS.toSeconds(endDelete - startDelete));
        LOGGER.info("Result: {}", results);

        ResultWriter resultWriter = new ResultWriter(TestUtils.getPerformanceTestLogsPath(TestInfo.getInstance().getActualTest()));
        resultWriter.write(results);
    }
}
