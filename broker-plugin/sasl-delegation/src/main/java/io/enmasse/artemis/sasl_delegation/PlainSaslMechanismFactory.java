/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.artemis.sasl_delegation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class PlainSaslMechanismFactory  implements SaslMechanismFactory {

    @Override
    public String getName() {
        return "PLAIN";
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public SaslMechanism newInstance(CallbackHandler callbackHandler,
                                     Map<String, ?> sharedState,
                                     Map<String, ?> options) {


        NameCallback nameCallback = new NameCallback("Username: ");
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(new Callback[] { nameCallback, passwordCallback });
        } catch (IOException ioe) {
            return null;
        } catch (UnsupportedCallbackException e) {
            return null;
        }

        return new PlainMechanism(nameCallback.getName(), passwordCallback.getPassword());
    }

    private static class PlainMechanism implements SaslMechanism {

        private final byte[] initialResponse;
        private boolean complete;

        public PlainMechanism(String name, char[] password) {
            ByteBuffer encodedPassword = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
            ByteBuffer nameBytes = StandardCharsets.UTF_8.encode(name);
            final int encodedNameLength = nameBytes.remaining();
            initialResponse = new byte[2 + encodedPassword.remaining() + encodedNameLength];
            nameBytes.get(initialResponse, 1, encodedNameLength);
            encodedPassword.get(initialResponse, 2+encodedNameLength, encodedPassword.remaining());
        }

        @Override
        public byte[] getResponse(byte[] challenge) {
            if(complete) {
                return new byte[0];
            } else {
                complete = true;
                return initialResponse;
            }

        }

        @Override
        public boolean completedSuccessfully() {
            return complete;
        }
    }
}
