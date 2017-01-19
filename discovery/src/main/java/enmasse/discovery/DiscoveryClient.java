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

package enmasse.discovery;

import java.util.Map;
import java.util.Optional;

/**
 * A client for talking to the 'podsense' discovery service a given set of labels, and notifying listeners of
 * the changing set of hosts.
 */
public class DiscoveryClient extends BaseDiscoveryClient {

    public DiscoveryClient(Endpoint endpoint, Map<String, String> labelFilter, Optional<String> containerName) {
        super(endpoint, "podsense", labelFilter, containerName);
    }

    public DiscoveryClient(Map<String, String> labelFilter, Optional<String> containerName) {
        super("podsense", labelFilter, containerName);
    }
}
