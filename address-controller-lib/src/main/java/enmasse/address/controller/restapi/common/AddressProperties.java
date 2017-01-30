package enmasse.address.controller.restapi.common;

/**
 * API type representing an address and its properties
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressProperties that = (AddressProperties) o;

        if (store_and_forward != that.store_and_forward) return false;
        if (multicast != that.multicast) return false;
        return flavor != null ? flavor.equals(that.flavor) : that.flavor == null;
    }

    @Override
    public int hashCode() {
        int result = (store_and_forward ? 1 : 0);
        result = 31 * result + (multicast ? 1 : 0);
        result = 31 * result + (flavor != null ? flavor.hashCode() : 0);
        return result;
    }
}
