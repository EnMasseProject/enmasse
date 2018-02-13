/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.clients;

public enum ClientType {
    CLI_RHEA_SENDER,
    CLI_RHEA_RECEIVER,
    CLI_RHEA_CONNECTOR,
    CLI_PROTON_PYTHON_SENDER,
    CLI_PROTON_PYTHON_RECEIVER,
    CLI_JAVA_PROTON_JMS_SENDER,
    CLI_JAVA_PROTON_JMS_RECEIVER,
    CLI_JAVA_ARTEMIS_JMS_SENDER,
    CLI_JAVA_ARTEMIS_JMS_RECEIVER,
    CLI_JAVA_OPENWIRE_JMS_SENDER,
    CLI_JAVA_OPENWIRE_JMS_RECEIVER;

    /**
     * Get bind client type to client executable
     *
     * @param client client type
     * @return client executable
     */
    public static String getCommand(ClientType client) {
        switch (client) {
            case CLI_RHEA_SENDER:
                return isWindows() ? "cli-rhea-sender.cmd" : "cli-rhea-sender";
            case CLI_RHEA_RECEIVER:
                return isWindows() ? "cli-rhea-receiver.cmd" : "cli-rhea-receiver";
            case CLI_RHEA_CONNECTOR:
                return isWindows() ? "cli-rhea-connector.cmd" : "cli-rhea-connector";
            case CLI_PROTON_PYTHON_SENDER:
                return "cli-proton-python-sender";
            case CLI_PROTON_PYTHON_RECEIVER:
                return "cli-proton-python-receiver";
            case CLI_JAVA_PROTON_JMS_SENDER:
                return "./client_executable/cli-qpid-jms.jar";
            case CLI_JAVA_PROTON_JMS_RECEIVER:
                return "./client_executable/cli-qpid-jms.jar";
            case CLI_JAVA_ARTEMIS_JMS_SENDER:
                return "./client_executable/cli-artemis-jms.jar";
            case CLI_JAVA_ARTEMIS_JMS_RECEIVER:
                return "./client_executable/cli-artemis-jms.jar";
            case CLI_JAVA_OPENWIRE_JMS_SENDER:
                return "./client_executable/cli-activemq.jar";
            case CLI_JAVA_OPENWIRE_JMS_RECEIVER:
                return "./client_executable/cli-activemq.jar";
            default:
                return "";
        }
    }

    public static String getAddressName(AbstractClient client) {
        switch (client.getClientType()) {
            case CLI_RHEA_SENDER:
            case CLI_RHEA_RECEIVER:
            case CLI_RHEA_CONNECTOR:
                return "cli_rhea";
            case CLI_PROTON_PYTHON_SENDER:
            case CLI_PROTON_PYTHON_RECEIVER:
                return "cli_python";
            case CLI_JAVA_PROTON_JMS_SENDER:
            case CLI_JAVA_PROTON_JMS_RECEIVER:
                return "cli_jms";
            case CLI_JAVA_ARTEMIS_JMS_SENDER:
            case CLI_JAVA_ARTEMIS_JMS_RECEIVER:
                return "cli_artemis";
            case CLI_JAVA_OPENWIRE_JMS_SENDER:
            case CLI_JAVA_OPENWIRE_JMS_RECEIVER:
                return "cli_openwire";
            default:
                return "";
        }
    }

    /**
     * Check os
     *
     * @return true if os is windows
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.contains("win"));
    }
}
