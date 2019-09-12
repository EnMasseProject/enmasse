This folder, for simplicity, stores one truststore and one keystore needed to deploy the amq-broker to openshift, the password is 123456 for both of them.

Steps:
- Create a secret with the provided keystore and truststore
oc create secret generic amq-app-secret --from-file=broker.ks --from-file=broker.ts
- Apply the template
oc process -f amq-broker-73-ssl.yaml -p AMQ_USER=admin -p AMQ_PASSWORD=admin -p AMQ_TRUSTSTORE_PASSWORD=123456 -p AMQ_KEYSTORE_PASSWORD=123456 -p AMQ_QUEUES=queue1,queue2 | oc create -f -