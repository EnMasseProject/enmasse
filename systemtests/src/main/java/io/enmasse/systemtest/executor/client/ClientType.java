package io.enmasse.systemtest.executor.client;

public enum ClientType {
    CLI_RHEA_SENDER,
    CLI_RHEA_RECEIVER,
    CLI_PROTON_PYTHON_SENDER,
    CLI_PROTON_PYTHON_RECEIVER,
    CLI_JAVA_SENDER,
    CLI_JAVA_REVEIVER;

    /**
     * Get bind client type to client executable
     * @param client client type
     * @return client executable
     */
    public static String getCommand(ClientType client){

        switch (client){
            case CLI_RHEA_SENDER:
                return isWindows() ? "cli-rhea-sender.cmd" : "cli-rhea-sender";
            case CLI_RHEA_RECEIVER:
                return isWindows() ? "cli-rhea-receiver.cmd" : "cli-rhea-receiver";
            default:
                return "";
            //TODO: complete for every client (cli-java, cli-proton-python)
        }
    }

    /**
     * Check os
     * @return true if os is windows
     */
    private static boolean isWindows(){
        String os = System.getProperty("os.name").toLowerCase();
        return (os.contains("win"));
    }
}
