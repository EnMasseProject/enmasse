/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale;

import io.enmasse.systemtest.model.address.AddressType;

/**
 * Framework assumes that all addresses passed to a messaging-client are of the same type
 */
public class ScaleTestClientConfiguration {

    private ScaleTestClientType clientType;
    private String clientId;
    private AddressType addressesType;

    private String hostname;
    private int port;
    private String username;
    private String password;
    private String[] addresses;

    //messaging-client
    private Integer linksPerConnection;

    //tenant-client
    private Integer addressesPerTenant;
    private Integer sendMessagePeriod; //milliseconds

    public ScaleTestClientConfiguration() {
		// empty
	}

	public ScaleTestClientType getClientType() {
        return clientType;
    }

    public void setClientType(ScaleTestClientType clientType) {
        this.clientType = clientType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public AddressType getAddressesType() {
        return addressesType;
    }

    public void setAddressesType(AddressType addressesType) {
        this.addressesType = addressesType;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String[] getAddresses() {
		return addresses;
	}

    public Integer getLinksPerConnection() {
    	return linksPerConnection;
    }

    public Integer getAddressesPerTenant() {
        return addressesPerTenant;
    }

    public Integer getSendMessagePeriod() {
        return sendMessagePeriod;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAddresses(String... addresses) {
        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("Addresses cannot be null or empty");
        }
        this.addresses = addresses;
    }

    public void setLinksPerConnection(Integer linksPerConnection) {
        this.linksPerConnection = linksPerConnection;
    }

    public void setAddressesPerTenant(Integer addressesPerTenant) {
        this.addressesPerTenant = addressesPerTenant;
    }

    public void setSendMessagePeriod(Integer sendMessagePeriod) {
        this.sendMessagePeriod = sendMessagePeriod;
    }

}
