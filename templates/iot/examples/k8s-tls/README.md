# Inter service certificates example

This example helps manually creating inter service certificates for the
EnMasse IoT components.

**Note:** When running on OpenShift the service CA is being used for this
          by default. However you can still override this in the way described
          here.

**Note:** The content in this directory serves as example only! If you want to run
          this in production, you need to create your own key & certificate setup,
          based on your own requirements.

## Creating keys & certificates

In order to create a new a new service CA, including keys and certificates for the
different services execute the following command from inside this directory:

    ./create

This will create a new set of key material in the directory `./build`.

## Deploy keys & certificates

Once you created the new key material, you can deploy it to the Kubernetes cluster.
For this to work you need to:

  * Have `kubectl` in your `$PATH`
  * Be logged in to the cluster you want to deploy to
  * Have the permissions to create new *secrets*

Run the following command to deploy:

    ./deploy

By default this will create new *secrets* in the `enmasse-infra` namespace. You
can use the following environment variables to change the deployment process:

  * `NAMESPACE` –  Change the namespace to deploy the secrets to. Defaults to `enmasse-infra`.
  * `PREFIX` – Add a prefix to all secrets. If you want to have a delimiter between the prefix
                and the actual name (like a dash), then you need to provide that as well.
                Defaults to an empty prefix. 

## Undeploy keys & certificates

You can undeploy the secrets using the following command:

    ./undeploy

For this to work you need to:

  * Have `kubectl` in your `$PATH`
  * Be logged in to the cluster you want to undeploy from
  * Have the permissions to delete *secrets*

You can use the same environment variables as described above.

## Configuring IoT infrastructure

You will need to configure the IoT infrastructure to let it know which
TLS key and certificates to use for each service.

The following example assumed that you deployed the keys & certificates to
the namespace `enmasse-infra` with the prefix `foo-`:

~~~yaml
apiVersion: iot.enmasse.io/v1alpha1
kind: IoTConfig
metadata:
  name: default
  namespace: enmasse-infra
spec:
  interServiceCertificates:
    secretCertificatesStrategy:
      caSecretName: foo-iot-service-ca
      serviceSecretNames:
        iot-auth-service: foo-iot-auth-service-tls
        iot-device-registry: foo-iot-device-registry-tls
        iot-tenant-service: foo-iot-tenant-service-tls
  adapters:
    http:
      endpoint:
        secretNameStrategy:
          secretName: foo-iot-http-adapter-tls
    mqtt:
      endpoint:
        secretNameStrategy:
          secretName: foo-iot-mqtt-adapter-tls
~~~
