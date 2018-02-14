/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */


package enmasse.mqtt;

/**
 * Provides information about client disconnection
 */
public class DisconnectionData {

    private String clientId;
    private boolean isError;

    /**
     * Constructor
     *
     * @param clientId  client identifier disconnected
     * @param isError   if there is a disconnection error
     */
    public DisconnectionData(String clientId, boolean isError) {
        this.clientId = clientId;
        this.isError = isError;
    }

    /**
     * Client identifier disconnected
     *
     * @return
     */
    public String clientId() {
        return this.clientId;
    }

    /**
     * If there is a disconnection error
     *
     * @return
     */
    public boolean isError() {
        return this.isError;
    }
}
