# Using EnMasse through the OpenShift Service Catalog

This document explains how to run and use EnMasse through the OpenShift Service Broker API and the Web Catalog UI. 
These instructions assume the use of `oc cluster up` to run a local OpenShift instance and must be adjusted accordingly 
when using other methods of running OpenShift.

## Setting up OpenShift and enabling the Catalog UI

- Initialize the OpenShift config files:
  - `oc cluster up && oc cluster down`
  - The config files should now be on your local machine at `/var/lib/origin/openshift.local.config/master/`
- Enable the Catalog UI
  - Create a file called `catalogui.js` in the directory mentioned above. Paste this into the file:
    ```
    window.OPENSHIFT_CONSTANTS.ENABLE_TECH_PREVIEW_FEATURE = {
      service_catalog_landing_page: true,
      pod_presets: true
    };
    
    window.OPENSHIFT_CONFIG.additionalServers = [{
      hostPort: "apiserver-service-catalog.127.0.0.1.nip.io",
      prefix: "/apis"
    }];
    ```
  - Edit the `master-config.yaml` (add a reference to `catalogui.js` as shown below):
    ```
    assetConfig:
    ...
      extensionScripts:
      - /var/lib/origin/openshift.local.config/master/catalogui.js
    ```
- Start the cluster (IMPORTANT: always use `--use-existing-config`, otherwise your files will be overwritten)
  - `oc cluster up --use-existing-config`

## Deploy the Service Catalog
- Login as `system:admin`
  - `oc login -u system:admin`
- Create a project for the service catalog:
  - `oc new-project service-catalog`
- Grant admin rights to the service catalog's service account:
  - `oc adm policy add-cluster-role-to-user admin system:serviceaccount:service-catalog:default`
- Deploy the Service Catalog resources
  - `curl -q https://gist.githubusercontent.com/jwforres/78d8c2a939e5e69e31ddd32471ce79fd/raw/518ffcd3139671f04aad9f21342f845c992f5543/gistfile1.txt | sed s/canary/v0.0.3/g | oc process -v CORS_ALLOWED_ORIGIN='.*' -f - | oc apply -f -`

## Deploy EnMasse
- Deploy EnMasse global infrastructure:
  - `bash <(curl https://raw.githubusercontent.com/EnMasseProject/enmasse/master/scripts/enmasse-deploy.sh) -c "https://localhost:8443" -u system:admin -p enmasse -m`
- Grant proper roles to enmasse service accounts:
  - `oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:enmasse:enmasse-service-account`
  - `oc adm policy add-cluster-role-to-user edit system:serviceaccount:enmasse:default`

## Register the EnMasse broker in the Service Catalog
- Get kubectl 1.6+ (older versions won't work with the Service Catalog API server):
  - `curl -o kubectl https://storage.googleapis.com/kubernetes-release/release/v1.6.0/bin/linux/amd64/kubectl ; chmod +x ./kubectl` (replace `linux` with `darwin` if using MacOS)
- Configure sc alias and make it connect to the Service Catalog API server:
  - `alias sc="kubectl --server=https://$(oc get route apiserver -n service-catalog -o jsonpath=\"{.spec.host}\") --insecure-skip-tls-verify"`
  - Make sure you can access the hostname returned by `oc get route apiserver -n service-catalog -o jsonpath="{.spec.host}"` (e.g. point it to OpenShift router's IP address in `/etc/hosts`)
- Register Broker in the Service Catalog:
  ```
  cat <<EOF | sc create -f -
  apiVersion: servicecatalog.k8s.io/v1alpha1
  kind: Broker
  metadata:
    name: maas-broker
  spec:
    url: http://address-controller.enmasse.svc.cluster.local:8080
  EOF
  ```
(Make sure there's no whitespace after EOF!)

At this point, the Service Catalog will contact the broker and retrieve the list of services the broker is providing. 

## Verifying if the broker is registered
- Check the status of the broker:
  - `sc get broker -o yaml`
  - The `status.conditions.message` should say "Successfully fetched catalog from broker"
- Check if there are four service classes:
  - `sc get serviceclasses`
  - The list should include a "queue" and a "topic" class as well as two "direct-*" classes

## Provisioning addresses through the Catalog UI   

- Open https://apiserver-service-catalog.127.0.0.1.nip.io/ and confirm the security exception
- Open https://localhost:8443
- Login as developer:developer
  - NOTE: if after logging in, you get redirected back to the login page, you didn't confirm the security exception in the first step of this section)
- You should see the queue, topic, direct-anycast-network and direct-multicast-network services in the catalog.
- Click on "queue"
- Select the "vanilla-queue" plan & click next
- Select the project you'd like the service instance to be provisioned in (NOTE: the actual queue will be deployed in a different project; this is just where the ServiceInstance object will be created in)
  

## Provisioning addresses through the CLI

### Provisioning a queue 
- Create a new project/namespace:
  - `oc new-project my-messaging-project`
- Create the service instance:
  - `sc create -f https://raw.githubusercontent.com/EnMasseProject/service-broker/master/examples/service-catalog/instance-queue.yaml -n my-messaging-project`
- Check the service instance's status:
  - `sc get instances -n my-messaging-project -o yaml`
  - The `status.conditions.message` should show "The instance was provisioned successfully"
- Verify the MaaS infra pods and the broker pod have been created:
  - Login as admin:
    - `oc login -u admin`
  - List projects:
    - `oc get projects`
    - Find a project named like "_enmasse-63a14329_"
  - List pods in said project:
    - `oc get pods -n enmasse-63a14329`
    - One of the pods should be called "my-vanilla-queue-<something>"

### Binding the queue
- Create the binding:
  - `sc create -f https://raw.githubusercontent.com/EnMasseProject/service-broker/master/examples/service-catalog/binding-queue.yaml -n my-messaging-project`
- Verify the binding's status:
  - `sc get bindings -n my-messaging-project -o yaml`
  - The `status.conditions.message` property should show "Injected bind result"
- Verify the secret has been created:
  - `oc get secret my-vanilla-queue -o yaml`

### Unbinding the queue
- Delete the binding:
  - `sc delete binding my-vanilla-queue-binding`
- Verify the secret has been deleted:
  - `oc get secrets -n my-messaging-project`

### Provisioning a topic in the same network
- Create the service instance:
  - `sc create -f https://raw.githubusercontent.com/EnMasseProject/service-broker/master/examples/service-catalog/instance-topic.yaml -n my-messaging-project`
    
### Deprovisioning a queue
- Delete the instance object:
  - `sc delete instance my-vanilla-queue -n my-messaging-project`
- Verify the broker pod is terminating:
  - `oc get pods -n enmasse-63a14329`
  


