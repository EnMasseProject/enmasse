#!/bin/bash
set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

echo "Running smoke tests"
kubectl apply -f ${TEMPLATES}/install/bundles/enmasse
sleep 120
kubectl get pods
minikube status
kubectl get nodes

kubectl get pods -o yaml
kubectl get nodes -o yaml
exit 1
