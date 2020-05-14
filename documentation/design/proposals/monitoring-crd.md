# Monitoring CRD

A new monitoring CRD should allow service admin to enable and configure monitoring for EnMasse. This proposal describes how the monitoring custom resource will function in relation to shared infra.


## Current Monitoring Setup

EnMasse monitoring is currenlty *all-or-nothing*. I.e. All metrics, alerts and dashboards are enabled via a flag, `ENABLE_MONITORING`, during installation.

There is no point in replicating this functionality using a Monitoring CRD:

    - apiVersion: monitoring.enmasse.io/v1alpha1
      kind: EnmasseMonitoring
      metadata:
        name: default-monitoringf
        namespace: enmasse-infra
      spec:
        monitoringEnabled: true


The value in creating a monitoring CRD is to allow more control over monitoring

## User Stories 

### User Story 1
* I want to be able to enable/disable metrics on a per infrastructure level e.g. Enabled for prod but disabled for dev
* I want to be able to enable/disable alerts on a per infrastructure level e.g. Enabled for prod but disabled for dev
* I want to be able to choose what dashboards are created

## Proposal

### Metrics

Service admin can enable monitoring of the EnMasse infrastructure via a monitoring CR in the enmasse-infra namespace. The will create servicemonitors such as the current `enmasse-operator-metrics`.


Service admins can further choose which MessagingInfras to monitor [(user Story 1)](#user-stories) be creating a monitoring CR for the MessaagingInfra. Monitoring CR's should have a one-to-one relationship wich MessagingInfra

An monitoring CR with an empty spec will enable scraping of all metrics for the associated MessagingInfra. When the CR is created, servicemonitors to monitor broker and router metrics will be created. They will use label selectors to ensure only metrics from the relevant brokers and metrics are scarped. 

    - apiVersion: monitoring.enmasse.io/v1alpha1
      kind: EnmasseMonitoring
      metadata:
        name: default-monitoring
        namespace: enmasse-infra
      spec:
        defaultMonitoring: true


This should be achieved using the namespace selector field of service monitors. I.e. when a messainginfra is added to the monitoring CR, the relevant service monitors should be created with the namespace specified in the namespace selector.

    - apiVersion: monitoring.enmasse.io/v1alpha1
      kind: EnmasseMonitoring
      metadata:
        name: myinfra-monitoring
        namespace: enmasse-infra
      spec:
        MessagingInfra: myinfra


### Alerts

Monitoring CRs can also be used to configure alerts. [(user Story 2)](#user-stories) When a monitoring Cr is create the relevant PrometheusRules CR will be create. Labels will be used in the alert expression to ensure the alerts are specific to the MessagingInfra.

The following configurations would be possible:

#### Empty alert field in monitoring CR

    - apiVersion: monitoring.enmasse.io/v1alpha1
      kind: EnmasseMonitoring
      metadata:
        name: myinfra-monitoring
        namespace: enmasse-infra
      spec:
        MessagingInfra: myinfra
        alerts:

In this scenario all alerts are created with default values. 


#### Alert entry created to modify an alert:

    - apiVersion: monitoring.enmasse.io/v1alpha1
      kind: EnmasseMonitoring
      metadata:
        name: myinfra-monitoring
        namespace: enmasse-infra
      spec:
        MessagingInfra: myinfra
        alerts:
          routermesh:
            time: 300s
          brokerMemoryUsage:
            value: 90

In this example the default time for the `routermesh` alert to fire is overwritten with a value of 300s. Similarly the default threshould value for `brokermemoryusage` will be overwritten with 90.

#### Disable an alert

    - apiVersion: monitoring.enmasse.io/v1alpha1
      kind: EnmasseMonitoring
      metadata:
        name: myinfra-monitoring
        namespace: enmasse-infra
      spec:
        MessagingInfra: myinfra
        alerts:
          routermesh:
            disabled: true

In this scenario the `routermesh` will be disabled so that it never fires.


### Issues
What about hosted service? This would mean hosts would have to configure monitoring as a customer created resources. This is clearly not an option!

#### Proposed solution

A flag similar to our current one will be included in the enmasse-operator (i.e. `ENABLE_MONITORING`). If set to true, an empty Monitoring CR will be created with eash MessagingInfra as it is create. Recall, this means that all metrics will be scraped and all alerts will be enabled for the MessagingInfra. The service hosts can then fine-tune and/or disable and metrics they like for each MessagingInfra by modifying/deleting the monitoring CRs.

Alternatively, the `ENABLE_MONITORING` flag can be set to false. No monitoring CRs will be created and therefore no metrics will be scraped and no alerts created. The service host can then enable any metrics they like be creating monitoring CRs.