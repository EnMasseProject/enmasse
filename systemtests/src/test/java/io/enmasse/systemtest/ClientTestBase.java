package io.enmasse.systemtest;

import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import org.junit.After;


public abstract class ClientTestBase extends BrokeredTestBase {
    protected ArgumentMap arguments = new ArgumentMap();

    @After
    public void teardownClient(){
        arguments.clear();
    }

    protected String getTopicPrefix(boolean topicSwitch){
        return topicSwitch ? "topic://" : "";
    }

    private String getAmqpsRoute(AddressSpace addressSpace){
        return getRouteEndpoint(defaultAddressSpace).toString();
    }

    private String getOpenwireRoute(AddressSpace addressSpace){
        return kubernetes.getEndpoint(addressSpace.getNamespace(), "messaging", "openwire").toString();
    }

    private String getCoreRoute(AddressSpace addressSpace){
        return kubernetes.getEndpoint(addressSpace.getNamespace(), "messaging", "core").toString();
    }

    protected String getRoute(AddressSpace addressSpace, AbstractClient client){
        switch (client.getClientType()){
            case CLI_RHEA_SENDER:
                return getAmqpsRoute(addressSpace);
            case CLI_RHEA_RECEIVER:
                return getAmqpsRoute(addressSpace);
            case CLI_PROTON_PYTHON_SENDER:
                return getAmqpsRoute(addressSpace);
            case CLI_PROTON_PYTHON_RECEIVER:
                return getAmqpsRoute(addressSpace);
            case CLI_JAVA_PROTON_JMS_SENDER:
                return getAmqpsRoute(addressSpace);
            case CLI_JAVA_PROTON_JMS_RECEIVER:
                return getAmqpsRoute(addressSpace);
            case CLI_JAVA_OPENWIRE_JMS_SENDER:
                return getOpenwireRoute(addressSpace);
            case CLI_JAVA_OPENWIRE_JMS_RECEIVER:
                return getOpenwireRoute(addressSpace);
            case CLI_JAVA_ARTEMIS_JMS_SENDER:
                return getCoreRoute(addressSpace);
            case CLI_JAVA_ARTEMIS_JMS_RECEIVER:
                return getCoreRoute(addressSpace);
            default:
                return "";
        }
    }
}
