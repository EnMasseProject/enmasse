package enmasse.storage.controller.model;

/**
 * @author Ulf Lilleengen
 */
public class Port {
    private final String name;
    private final int portNum;

    public Port(String name, int portNum) {
        this.name = name;
        this.portNum = portNum;
    }

    public String name() {
        return this.name;
    }

    public int port() {
        return this.portNum;
    }
}
