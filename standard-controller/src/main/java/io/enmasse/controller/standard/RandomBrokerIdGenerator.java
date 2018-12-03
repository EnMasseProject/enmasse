/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.util.Random;

public class RandomBrokerIdGenerator implements BrokerIdGenerator {
    private final Random random = new Random(System.currentTimeMillis());
    private static final String ALPHA_NUMERIC_STRING = "abcdefghijklmnopqrstuvwxyz0123456789";

    public String generateBrokerId() {
        return generateAlphaNumberic(4);
    }

    private String generateAlphaNumberic(int length) {
        StringBuilder builder = new StringBuilder();
        while (length-- != 0) {
            int character = (int)(random.nextDouble()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }
}
