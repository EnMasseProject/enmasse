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

package enmasse.storage.controller.openshift;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Comparsion rules for openshift resources to ensure that they are updated in the correct order.
 */
public class ResourceComparator implements Comparator<IResource> {
    private static final List<String> kindOrder = Arrays.asList(ResourceKind.DEPLOYMENT_CONFIG, ResourceKind.REPLICATION_CONTROLLER, ResourceKind.POD);

    @Override
    public int compare(IResource o1, IResource o2) {
        String k1 = o1.getKind();
        String k2 = o2.getKind();
        if (k1.equals(k2)) {
            return 0;
        } else {
            int k1Pos = 0;
            int k2Pos = 0;

            for (int i = 0; i < kindOrder.size(); i++) {
                if (kindOrder.get(i).equals(k1)) {
                    k1Pos = i;
                }
                if (kindOrder.get(i).equals(k2)) {
                    k2Pos = i;
                }
            }
            if (k1Pos < k2Pos) {
                return -1;
            } else if (k1Pos > k2Pos) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
