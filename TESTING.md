# Running enmasse test suite

This document gives a detailed breakdown of the testing processes and testing options for EnMasse within system tests. 

## Setting up test environment

To run any system tests you need a Kubernetes or Openshift cluster available in your active kubernetes context. 
You can use [minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/), [minishift](https://www.okd.io/minishift/), [oc cluster up](https://github.com/openshift/origin) or [CodeReady Contaners](https://github.com/code-ready/crc) to have access to a cluster on your local machine. 
You can also access a remote cluster on any machine you want. Make sure `oc/kubectl` is logged in to your cluster with `cluster-admin` privileges. However you should not run the tests with `system:admin` user. You can give the privileges to a standard user :

```shell script
oc login -u system:admin https://localhost:8443
oc adm policy add-cluster-role-to-user cluster-admin developer
oc login -u developer -p developer https://localhost:8443
```

Systemtests framework installs enmasse and iot operators by default with latest templates and images, you can also generate templates yourself into templates/build/enmasse-latest
or you can use downloaded templates of release, in this case you need to provide custom path to your install bundle via env variable TEMPLATES

#### Generate templates

```shell script
make templates
```

#### Provide custom template path

```shell script
export TEMPLATES="/path/to/your/install/bundle"
```

## Running all tests

```shell script
make PROFILE=${PROFILE} systemtests
```

##  Running a single test class

```shell script
make PROFILE=${PROFILE} TESTCASE="**.SmokeTest" systemtests
```

Where $PROFILE can be:
* systemtests
* soak
* iot
* shared
* isolated
* shared-iot
* isolated-iot
* smoke
* smoke-iot
* upgrade
* olm

## Running upgrade test

```shell script
mkdir templates/build -pv
export START_VERSION=0.28.2
wget https://github.com/EnMasseProject/enmasse/releases/download/${START_VERSION}/enmasse-${START_VERSION}.tgz -O templates/build/enmasse-${START_VERSION}.tgz
tar zxvf templates/build/enmasse-${version}.tgz -C templates/build
make templates
make TAG=latest imageenv
export START_TEMPLATES=${pwd}/templates/build/enmasse-${START_VERSION}
export UPGRADE_TEMPLATES=${pwd}/templates/build/enmasse-latest
make PROFILE=upgrade systemtests
```

## Test process
1. Tests are stored into TestPlan
2. [OperatorManager](systemtests/src/main/java/io/enmasse/systemtest/operator/OperatorManager.java) installs enmasse operator, example plans, example auth services and roles (if test is iot is also deploy iot components).
3. Test is triggered and evaluated.
4. If test is latest [OperatorManager](systemtests/src/main/java/io/enmasse/systemtest/operator/OperatorManager.java) uninstall operators and clean kubernetes env, else continue with step 2.

## Creating test cases
* Decide if you can create test under existing test class [here](systemtests/src/test/java/io/enmasse/systemtest) or create a new one.
* If your test can work with shared address space please store test in [shared](systemtests/src/test/java/io/enmasse/systemtest/shared) package of not pick other
* Create `@Test` method:
    * Create resources using builders (`AddressSpaceBuilder`, `AddressBuilder` etc...) from api-model
    * call resourceManager methods for creating enmasse resources in kubernetes cluster `resourceManager.createAddressSpace(addressSpace)`
    * do test code
* You don't need to take care about removing resources, Test callbacks do that after every test

## Available env variable for systemtests

We can configure our system tests with several environment variables, which are loaded before test execution. 
All environment variables can be seen in [Environment](systemtests/src/main/java/io/enmasse/systemtest/Environment.java) class:

| Name                      | Description                                                                          | Default                                          |
| :-----------------------: | :----------------------------------------------------------------------------------: | :----------------------------------------------: |
| TEST_LOGDIR                | Folder where log information and kubernetes state are stored                          | /tmp/testlogs                                          |
| KUBERNETES_NAMESPACE                | kubernetes namespace where enmasse infra is deployed                                              | enmasse-infra                                           |
| KUBERNETES_API_URL           | kubernetes api url                                         | got from actual context                                       |
| KUBERNETES_API_TOKEN         | kubernetes token of cluster-admin user                                       | got from actual context |
| KUBERNETES_DOMAIN              | dns domain name of kubernetes cluster                                      | nip.io            |
| KUBERNETES_API_CONNECT_TIMEOUT              | kuberntes api connection timeout                                  | 60                      |
| KUBERNETES_API_READ_TIMEOUT          | kuberntes api connection timeout                                 | 60                                           |
| KUBERNETES_API_WRITE_TIMEOUT | kuberntes api connection timeout                                                       | 60                                            |
| UPGRADE_TEMPLATES         | path for upgrade templates                                                                       | ${ENMASSE_DIR}/templates/build/enmasse-latest                                       |
| START_TEMPLATES      | path for start templates before upgrade                                     | ${ENMASSE_DIR}/templates/build/enmasse-latest             |
| TEMPLATES         | path where templates are stored                    | ${ENMASSE_DIR}/templates/build/enmasse-latest                                        |
| SKIP_CLEANUP             | skip teardown and clean phase of tests (for debug only)                            | false                                            |
| SKIP_UNINSTALL         | skip enmassse operator uninstall (for debug purpose)                                                                    | false                                     |
| STORE_SCREENSHOTS         | store screenshots in selenium tests even if test failed or not                                                                    | false                                     |
| MONITORING_NAMESPACE         | Inamespace for install monitoring part of enmasse                                                                    | enmasse-monitoring                                     |
| TAG         | tag of images                                                                    | latest                                     |
| PRODUCT_NAME         | name of product                                                                    | enmasse                                     |
| INSTALL_TYPE         | type of installation enmasse operator                                                                   | BUNDLE                                     |
| OLM_INSTALL_TYPE         | type of olm installation enmasse operator                                                                    | SPECIFIC                                     |
| SKIP_DEPLOY_INFINISPAN         | skip deployment of infinispan iot component                                                                    | false                                     |
| INFINISPAN_PROJECT         | project where infinispan deployment is deployed                                                                   | systemtests-infinispan                                     |
