# Device X509 Client Example

## Create a trust anchor

Run the script:

    ./create

## Assign the trust anchor to a tenant

Add the trust anchor to the `IoTProject`:

    oc patch iotproject iot --type merge -p "{\"spec\": {\"configuration\": {\"trustAnchors\": [{\"certificate\": \"$(cat build/ca-cert.pem | sed 's/$/\\n/g' | tr -d '\n')\"}]}}}"

## Create a new device

    hat device create device1
    hat creds enable-x509 device1 "CN=device1,OU=IoT,O=EnMasse"

## Test with `hot`

    hot publish mqtt telemetry ssl://iot-mqtt-adapter-enmasse-infra.apps.your.cluster:443 enmasse-infra.iot device1 '{}' --client-cert build/device1-fullchain.crt --client-key build/device1.key
