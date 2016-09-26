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

package enmasse.config.bridge.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;

import java.net.URL;

/**
 * Openshift client with a set namespace.
 */
public class OpenshiftClient {
    private final IClient client;
    private final String namespace;

    public OpenshiftClient(IClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    public IWatcher watch(IOpenShiftWatchListener listener, String ... kinds) {
        return client.watch(namespace, listener, kinds);
    }

    public URL getBaseURL() {
        return client.getBaseURL();
    }
}
