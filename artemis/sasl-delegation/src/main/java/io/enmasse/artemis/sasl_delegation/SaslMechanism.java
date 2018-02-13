/*
 * Copyright 2017, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.artemis.sasl_delegation;

interface SaslMechanism {

    byte[] getResponse(byte[] challenge);
    boolean completedSuccessfully();
}
