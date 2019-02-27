# Start and test managed IoT Project

* Log in to user with cluster admin privileges
* Create project

  ```
  oc new-project enmasse-infra || oc project enmasse-infra
  ```

* Install EnMasse with IoT services

  ```
  oc apply -f templates/build/enmasse-latest/install/components/standard-authservice
  oc apply -f templates/build/enmasse-latest/install/components/example-roles
  oc apply -f templates/build/enmasse-latest/install/components/example-plans
  oc apply -f templates/build/enmasse-latest/install/bundles/enmasse
  oc apply -f templates/build/enmasse-latest/install/bundles/iot
  ```

* Wait for the EnMasse installation to succeed. The following command should show all pods to be ready:

  ```
  oc get pods
  ```

* Switch back to a non-admin user

* Create a new project

  ```
  oc new-project my-iot-1
  ```

* Create Managed IoT Project

  ```
  oc create -f iot/examples/iot-project-managed.yaml
  ```

* Create Messaging Consumer User

  ```
  sed -e 's/enmasse-infra/my-iot-1/g' iot/examples/iot-user.yaml | oc apply -f -
  ```
  
  **Note**: If your project name is not `my-iot-1`, then you need to replace this in the previous command accordingly.

* Register a device

  ```
  curl --insecure -X POST -i -H 'Content-Type: application/json' --data-binary '{"device-id": "4711"}' https://$(oc get routes device-registry --template='{{ .spec.host }}')/registration/my-iot-1.managed
  ```

* Add credentials for a device

  ```
  PWD_HASH=$(echo -n "hono-secret" | openssl dgst -binary -sha512 | base64 | tr -d '\n')
  
  curl --insecure -X POST -i -H 'Content-Type: application/json' --data-binary '{"device-id": "4711","type": "hashed-password","auth-id": "sensor1","secrets": [{"hash-function" : "sha-512","pwd-hash":"'$PWD_HASH'"}]}' https://$(oc get routes device-registry --template='{{ .spec.host }}')/credentials/my-iot-1.managed
  ```

* Start a telemetry consumer

  ```
  oc get addressspace managed -o jsonpath={.status.endpointStatuses[?\(@.name==\'messaging\'\)].cert} | base64 --decode > target/config/hono-demo-certs-jar/tls.crt
  
  cd cli
  mvn spring-boot:run -Drun.arguments=--hono.client.host=$(oc get addressspace managed -o jsonpath={.status.endpointStatuses[?\(@.name==\'messaging\'\)].externalHost}),--hono.client.port=443,--hono.client.username=consumer,--hono.client.password=foobar,--tenant.id=my-iot-1.managed,--hono.client.trustStorePath=target/config/hono-demo-certs-jar/tls.crt
  ```

* Send telemetry message using HTTP

  ```
  curl --insecure -X POST -i -u sensor1@my-iot-1.managed:hono-secret -H 'Content-Type: application/json' --data-binary '{"temp": 5}' https://$(oc get route iot-http-adapter --template='{{.spec.host}}')/telemetry
  ```

* Send telemetry message using MQTT

  This is an example using NodePort, so you need to know your cluster IP and use in the command

  ```
  mosquitto_pub -h ${cluster.ip} -p 31883 -u 'sensor1@my-iot-1.managed' -P hono-secret -t telemetry -m '{"temp": 5}'
  ```

  In the future, we'll provide a proper examples based on the Openshift route with TLS and remove NodePort.

## Work with HAT – Hono Admin Tool

* Download and install `hat` from https://github.com/ctron/hat/releases
* Create a new context:

  ```
  hat context create my-iot-1 --default-tenant my-iot-1.managed https://$(oc -n enmasse-infra get routes device-registry --template='{{ .spec.host }}')
  ```

* Register a new device

  ```
  hat reg create 4711
  ```

* Add credentials for a device

  ```
  hat cred set-password sensor1 sha-512 hono-secret --device 4711
  ```

