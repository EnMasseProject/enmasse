#!/usr/bin/env bash

# Try twice, since order matters
microk8s kubectl apply -f https://github.com/operator-framework/operator-lifecycle-manager/releases/download/0.11.0/crds.yaml
microk8s kubectl apply -f https://github.com/operator-framework/operator-lifecycle-manager/releases/download/0.11.0/olm.yaml

# Delete "operatorhubio-catalog"
microk8s kubectl delete catalogsource operatorhubio-catalog -n olm


# Install OPM tool
curl -o opm -L https://github.com/operator-framework/operator-registry/releases/download/v1.13.7/linux-amd64-opm
chmod 755 opm
sudo mv opm /usr/bin
