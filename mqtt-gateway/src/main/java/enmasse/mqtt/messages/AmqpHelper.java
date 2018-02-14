/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.messages;

/**
 * Helper class for AMQP side
 */
public final class AmqpHelper {

    public static final String AMQP_CLIENT_CONTROL_ADDRESS_TEMPLATE = "$mqtt.to.%s.control";
    public static final String AMQP_CLIENT_PUBLISH_ADDRESS_TEMPLATE = "$mqtt.to.%s.publish";
    public static final String AMQP_CLIENT_PUBREL_ADDRESS_TEMPLATE = "$mqtt.%s.pubrel";

    /**
     * Extract client identifier from the unique client control address
     *
     * @param address   the address
     * @return  the client identifier
     */
    public static String getClientIdFromControlAddress(String address) {

        return address.substring(address.indexOf("$mqtt.to.") + "$mqtt.to.".length(), address.indexOf(".control"));
    }

    /**
     * Return the unique client control address from the client identifier
     *
     * @param clientId  the client identifier
     * @return  the address
     */
    public static String getControlClientAddress(String clientId) {

        return String.format(AMQP_CLIENT_CONTROL_ADDRESS_TEMPLATE, clientId);
    }

    /**
     * Extract client identifier from the unique client publish address
     *
     * @param address   the address
     * @return  the client identifier
     */
    public static String getClientIdFromPublishAddress(String address) {

        return address.substring(address.indexOf("$mqtt.to.") + "$mqtt.to.".length(), address.indexOf(".publish"));
    }

    /**
     * Return the unique client publish address from the client identifier
     *
     * @param clientId  the client identifier
     * @return  the address
     */
    public static String getPublishClientAddress(String clientId) {

        return String.format(AMQP_CLIENT_PUBLISH_ADDRESS_TEMPLATE, clientId);
    }

    /**
     * Return client identifier from the pubrel client address
     *
     * @param address   the address
     * @return  the client identifier
     */
    public static String getClientIdFromPubrelAddress(String address) {

        return address.substring(address.indexOf("$mqtt.") + "$mqtt.".length(), address.indexOf(".pubrel"));
    }
}
