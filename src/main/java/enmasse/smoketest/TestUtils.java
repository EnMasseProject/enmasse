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

package enmasse.smoketest;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TODO: Description
 */
public class TestUtils {
    public static void setReplicas(String controllerName, String address, int numReplicas, long timeout, TimeUnit timeUnit) throws InterruptedException {
        IReplicationController controller = Environment.client.get(ResourceKind.REPLICATION_CONTROLLER, controllerName, Environment.namespace);
        controller.setReplicas(numReplicas);
        Environment.client.update(controller);
        waitForNReplicas(address, numReplicas, timeout, timeUnit);
    }

    public static void waitForNReplicas(String address, int expectedReplicas, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        boolean done = false;
        int actualReplicas = 0;
        do {
            List<IPod> pods = Environment.client.list(ResourceKind.POD, Environment.namespace, Collections.singletonMap("address", address));
            actualReplicas = numReady(pods);
            System.out.println("Have " + actualReplicas + " out of " + pods.size() + " replicas. Expecting " + expectedReplicas);
            if (actualReplicas != pods.size() || actualReplicas != expectedReplicas) {
                Thread.sleep(5000);
            } else {
                done = true;
            }
        } while (System.currentTimeMillis() < endTime && !done);

        if (!done) {
            throw new RuntimeException("Only " + actualReplicas + " out of " + expectedReplicas + " in state 'Running' before timeout");
        }
    }

    private static int numReady(List<IPod> pods) {
        int numReady = 0;
        for (IPod pod : pods) {
            if ("Running".equals(pod.getStatus())) {
                numReady++;
            } else {
                System.out.println("POD " + pod.getName() + " in status : " + pod.getStatus());
            }
        }
        return numReady;
    }
}
