This folder stores all required files to deploy the amq-broker to openshift.
Note that for simplicity all required keystores and truststores are already generated, the password is 123456 for all of them.

Steps:
- Create a secret with the provided keystore and truststore
oc create secret generic amq-app-secret --from-file=broker.ks --from-file=broker.ts
- Apply the template
oc process -f amq-broker-73-ssl.yaml -p AMQ_USER=admin -p AMQ_PASSWORD=admin -p AMQ_TRUSTSTORE_PASSWORD=123456 -p AMQ_KEYSTORE_PASSWORD=123456 -p AMQ_QUEUES=queue1,queue2 | oc create -f -