/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.controller.auth;

import io.fabric8.kubernetes.api.model.DoneableSecret;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Base64;

/**
 * User database backed by kubernetes secrets
 */
public class SecretUserDatabase implements UserDatabase {
    private final OpenShiftClient client;
    private final String namespace;
    private final String secretName;
    private final Base64.Decoder decoder = Base64.getDecoder();

    private SecretUserDatabase(OpenShiftClient client, String namespace, String secretName) {
        this.client = client;
        this.namespace = namespace;
        this.secretName = secretName;
    }

    @Override
    public boolean hasUser(String userName) {
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        return secret != null && secret.getData() != null && secret.getData().containsKey(userName);
    }

    @Override
    public void addUser(String username, String password) {
        DoneableSecret secret = client.secrets().inNamespace(namespace).withName(secretName).edit();
        if (secret == null) {
            secret = client.secrets().inNamespace(namespace).withName(secretName).createNew();
        }
        secret.addToData(username, password);
        secret.done();
    }

    @Override
    public boolean authenticate(String username, String password) {
        Secret secret = client.secrets().inNamespace(namespace).withName(secretName).get();
        if (secret == null || secret.getData() == null) {
            return false;
        }
        String encodedPw = secret.getData().get(username);
        if (encodedPw == null) {
            return false;
        }

        String decoded = new String(decoder.decode(encodedPw));
        return password.equals(decoded);
    }

    public static UserDatabase create(OpenShiftClient client, String namespace, String secretName) {
        return new SecretUserDatabase(client, namespace, secretName);
    }
}
