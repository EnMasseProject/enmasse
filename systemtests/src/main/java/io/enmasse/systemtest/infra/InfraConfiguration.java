/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.infra;

import io.fabric8.kubernetes.api.model.PodTemplateSpec;

public class InfraConfiguration {

    //common to broker, router and admin components
    private String cpu;
    private String memory;
    private PodTemplateSpec templateSpec;

    //only router
    private Integer routerReplicas;

    //only broker
    private String brokerStorage;
    private String brokerJavaOpts;

    public static InfraConfiguration router(String cpu, String memory, PodTemplateSpec templateSpec, int routerReplicas) {
        InfraConfiguration config = new InfraConfiguration();
        config.setCpu(cpu);
        config.setMemory(memory);
        config.setTemplateSpec(templateSpec);
        config.setRouterReplicas(routerReplicas);
        return config;
    }

    public static InfraConfiguration broker(String cpu, String memory, PodTemplateSpec templateSpec, String brokerStorage, String brokerJavaOpts) {
        InfraConfiguration config = new InfraConfiguration();
        config.setCpu(cpu);
        config.setMemory(memory);
        config.setTemplateSpec(templateSpec);
        config.setBrokerStorage(brokerStorage);
        config.setBrokerJavaOpts(brokerJavaOpts);
        return config;
    }

    public static InfraConfiguration admin(String cpu, String memory, PodTemplateSpec templateSpec) {
        InfraConfiguration config = new InfraConfiguration();
        config.setCpu(cpu);
        config.setMemory(memory);
        config.setTemplateSpec(templateSpec);
        return config;
    }

    public String getCpu() {
        return cpu;
    }
    public void setCpu(String cpu) {
        this.cpu = cpu;
    }
    public String getMemory() {
        return memory;
    }
    public void setMemory(String memory) {
        this.memory = memory;
    }
    public PodTemplateSpec getTemplateSpec() {
        return templateSpec;
    }
    public void setTemplateSpec(PodTemplateSpec templateSpec) {
        this.templateSpec = templateSpec;
    }
    public Integer getRouterReplicas() {
        return routerReplicas;
    }
    public void setRouterReplicas(Integer routerReplicas) {
        this.routerReplicas = routerReplicas;
    }
    public String getBrokerStorage() {
        return brokerStorage;
    }
    public void setBrokerStorage(String brokerStorage) {
        this.brokerStorage = brokerStorage;
    }
    public String getBrokerJavaOpts() {
        return brokerJavaOpts;
    }
    public void setBrokerJavaOpts(String brokerJavaOpts) {
        this.brokerJavaOpts = brokerJavaOpts;
    }

}
