/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.artemis.sasl_delegation;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

interface SaslMechanismFactory {
    String getName();
    boolean isSecure();
    SaslMechanism newInstance(CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options);
}
