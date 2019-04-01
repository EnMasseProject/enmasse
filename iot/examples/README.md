# Start and test managed IoT Project

* Log in to user with cluster admin privileges
* Create project

  ```
  oc new-project enmasse-infra || oc project enmasse-infra
  ```

* Install EnMasse with IoT services

  ```
  oc apply -f templates/build/enmasse-latest/install/bundles/enmasse
  
  oc apply -f templates/build/enmasse-latest/install/components/example-authservices/standard-authservice.yaml
  oc apply -f templates/build/enmasse-latest/install/components/example-roles
  oc apply -f templates/build/enmasse-latest/install/components/example-plans
  
  oc apply -f templates/build/enmasse-latest/install/preview-bundles/iot
  oc apply -f iot/examples/iot-config.yaml
  ```

* Wait for the EnMasse installation to succeed. The following command should show all pods to be ready:

  ```
  oc get pods
  ```

* Switch back to a non-admin user

* Create Managed IoT Project

  ```
  oc new-project myapp || oc project myapp
  oc create -f iot/examples/iot-project-managed.yaml
  ```

* Wait for the resources to be ready

  ```
  oc get iotproject
  oc get addressspace
  ```

* Create Messaging Consumer User

  ```
  oc create -f iot/examples/iot-user.yaml
  ```

* Register a device

  ```
  curl --insecure -X POST -i -H 'Content-Type: application/json' --data-binary '{"device-id": "4711"}' https://$(oc -n enmasse-infra get routes device-registry --template='{{ .spec.host }}')/registration/myapp.iot
  ```

* Add credentials for a device

  ```
  curl --insecure -X POST -i -H 'Content-Type: application/json' --data-binary '{"device-id": "4711","type": "hashed-password","auth-id": "sensor1","secrets": [{"hash-function" : "sha-512","pwd-plain":"'hono-secret'"}]}' https://$(oc -n enmasse-infra get routes device-registry --template='{{ .spec.host }}')/credentials/myapp.iot
  ```

* Start a telemetry consumer

In Hono project run

  ```
  cd cli
  
  # at least once run
  mvn package -am

  oc -n myapp get addressspace iot -o jsonpath={.status.endpointStatuses[?\(@.name==\'messaging\'\)].cert} | base64 --decode > target/config/hono-demo-certs-jar/tls.crt

  mvn spring-boot:run -Drun.arguments=--hono.client.host=$(oc -n myapp get addressspace iot -o jsonpath={.status.endpointStatuses[?\(@.name==\'messaging\'\)].externalHost}),--hono.client.port=443,--hono.client.username=consumer,--hono.client.password=foobar,--tenant.id=myapp.iot,--hono.client.trustStorePath=target/config/hono-demo-certs-jar/tls.crt
  ```

* Send telemetry message using HTTP

  ```
  curl --insecure -X POST -i -u sensor1@myapp.iot:hono-secret -H 'Content-Type: application/json' --data-binary '{"temp": 5}' https://$(oc -n enmasse-infra get route iot-http-adapter --template='{{.spec.host}}')/telemetry
  ```

* Send telemetry message using MQTT

  This is an example using NodePort, so you need to know your cluster IP and use in the command

  ```
  mosquitto_pub -h $(oc -n enmasse-infra get route iot-mqtt-adapter --template='{{.spec.host}}') -p 443 -u 'sensor1@myapp.iot' -P hono-secret -t telemetry -m '{"temp": 5}' -i 4711 --cafile /etc/pki/tls/certs/ca-bundle.crt
  ```

  **Note:** You might need to change the `-cafile` parameter to match the certificate you used for the MQTT endpoint. The value `/etc/pki/tls/certs/ca-bundle.crt` points to the system wide
            trusted CA list on a standard RHEL/CentOS machine and will only work when you use a proper (not self-signed) certificate, like from Let's Encrypt.

  **Note:** For this to work you will need a Mosquitto CLI version which supports TLS SNI.

## Work with HAT – Hono Admin Tool

* Download and install `hat` from https://github.com/ctron/hat/releases
* Create a new context:

  ```
  hat context create myapp1 --default-tenant myapp.iot https://$(oc -n enmasse-infra get routes device-registry --template='{{ .spec.host }}')
  ```

* Register a new device

  ```
  hat reg create 4711
  ```

* Add credentials for a device

  ```
  hat cred set-password sensor1 sha-512 hono-secret --device 4711
  ```

