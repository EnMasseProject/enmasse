## Manually

Deploy all components manually:

    kubectl apply -f common
    kubectl apply -f manual

## Via Operator

Install the Infinispan operator, either via OLM, or manually.

Then deploy all components:

    kubectl apply -f common
    kubectl apply -f operator

## Register the protobuf schemas

Enable the indexing of entries by registering the protobuf schemas 
in the infinispan server :

    curl -u user:password -X POST --data-binary @./manual/deviceConnection.proto http://infinispan:8080/rest/___protobuf_metadata/deviceConnection.proto
    curl -u user:password -X POST --data-binary @./manual/deviceRegistry.proto http://infinispan:8080/rest/___protobuf_metadata/deviceRegistry.proto