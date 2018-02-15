package io.enmasse.systemtest.resources;

public class AddressResource {
    private String name;
    private double credit;

    public AddressResource(String name, double credit) {
        this.name = name;
        this.credit = credit;
    }

    public String getName() {
        return name;
    }

    public double getCredit() {
        return credit;
    }
}
