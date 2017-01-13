package enmasse.storage.controller.restapi;

/**
 * TODO: Description
 */
public class AddressProperties {
    public boolean store_and_forward;
    public boolean multicast;
    public String flavor;

    public AddressProperties() {}

    public AddressProperties(boolean store_and_forward, boolean multicast, String flavor) {
        this.store_and_forward = store_and_forward;
        this.multicast = multicast;
        this.flavor = flavor;
    }
}
