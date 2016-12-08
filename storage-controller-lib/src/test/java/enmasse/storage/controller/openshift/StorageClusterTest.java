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

import enmasse.storage.controller.admin.ClusterManager;
import enmasse.storage.controller.model.AddressType;
import enmasse.storage.controller.model.LabelKeys;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.openshift.api.model.*;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StorageClusterTest {

}
