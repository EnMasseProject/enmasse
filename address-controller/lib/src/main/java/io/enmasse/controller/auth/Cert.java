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

import java.io.File;

public class Cert {
    private final CertComponent component;
    private final File keyFile;
    private final File certFile;

    Cert(CertComponent component, File keyFile, File certFile) {
        this.component = component;
        this.keyFile = keyFile;
        this.certFile = certFile;
    }

    public CertComponent getComponent() {
        return component;
    }

    public File getKeyFile() {
        return keyFile;
    }

    public File getCertFile() {
        return certFile;
    }

    @Override
    public String toString() {
        return component.toString();
    }
}
