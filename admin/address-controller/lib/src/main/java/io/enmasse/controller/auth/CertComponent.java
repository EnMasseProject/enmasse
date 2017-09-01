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

public class CertComponent {
    private final String name;
    private final String namespace;
    private final String secretName;

    CertComponent(String name, String namespace, String secretName) {
        this.name = name;
        this.namespace = namespace;
        this.secretName = secretName;
    }

    public String getName() {
        return name;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "{name=" + name + "," +
                "secretName=" + secretName + "," +
                "namespace=" + namespace + "}";
    }
}
