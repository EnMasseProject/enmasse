/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.sharedinfra;

import io.enmasse.api.model.MessagingAddress;
import io.enmasse.api.model.MessagingAddressBuilder;
import io.enmasse.api.model.MessagingEndpoint;
import io.enmasse.api.model.MessagingEndpointBuilder;
import io.enmasse.api.model.MessagingProject;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.framework.TestPlanInfo;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingInfrastructure;
import io.enmasse.systemtest.framework.annotations.DefaultMessagingProject;
import io.enmasse.systemtest.framework.annotations.SkipResourceLogging;
import io.enmasse.systemtest.messaginginfra.resources.MessagingAddressResourceType;
import io.enmasse.systemtest.scale.ResultWriter;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.framework.TestTag.SCALE;

@Tag(SCALE)
@SkipResourceLogging
public class MessagingAddressPerfTest extends TestBase {
    private static final Logger LOGGER = LoggerUtils.getLogger();

    /**
     * Simple performance test to be able to track create and delete performance of addresses.
     */
    @Test
    @DefaultMessagingInfrastructure
    @DefaultMessagingProject
    public void testCreateDelete(ExtensionContext extensionContext) throws Exception {
        MessagingProject project = resourceManager.getDefaultMessagingProject();
        MessagingEndpoint endpoint = new MessagingEndpointBuilder()
                .editOrNewMetadata()
                .withNamespace(project.getMetadata().getNamespace())
                .withName("app")
                .endMetadata()
                .editOrNewSpec()
                .addToProtocols("AMQP")
                .editOrNewNodePort()
                .endNodePort()
                .endSpec()
                .build();
        resourceManager.createResource(extensionContext, endpoint);

        int numAnycast = 1000;
        int numQueues = 300;
        List<MessagingAddress> addresses = new ArrayList<>();

        for (int i = 0; i < numAnycast; i++) {
            addresses.add(new MessagingAddressBuilder()
                    .editOrNewMetadata()
                    .withNamespace(project.getMetadata().getNamespace())
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
                    .withNamespace(project.getMetadata().getNamespace())
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
        resourceManager.createResource(extensionContext, true, toCreate);
        long end = System.nanoTime();

        long startDelete = System.nanoTime();
        resourceManager.deleteResource(toCreate);
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

        ResultWriter resultWriter = new ResultWriter(TestUtils.getPerformanceTestLogsPath(TestPlanInfo.getInstance().getActualTest()));
        resultWriter.write(results);
    }
}
