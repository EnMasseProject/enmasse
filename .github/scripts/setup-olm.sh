#!/usr/bin/env bash

# Try twice, since order matters
kubectl apply -f https://github.com/operator-framework/operator-lifecycle-manager/releases/download/0.15.1/crds.yaml
kubectl apply -f https://github.com/operator-framework/operator-lifecycle-manager/releases/download/0.15.1/olm.yaml

# Delete "operatorhubio-catalog"
kubectl delete catalogsource operatorhubio-catalog -n olm
