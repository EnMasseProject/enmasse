#!/bin/bash
TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

sleep 10

echo "Processes"

ps auxww

echo "Running smoke tests"
kubectl apply -f ${TEMPLATES}/install/bundles/enmasse
sleep 120
kubectl get pods
minikube status
kubectl get nodes

echo "PWD"
pwd

echo "DU PWD"
du -sh *

echo "DU ROOT"
sudo du -sh /*

echo "DF"
df -h

echo "Check DU"
du -sh *

echo "Check local DU"
sudo du -a . | sort -n -r | head -n 20

echo "Check system DU"
sudo du -a / | sort -n -r | head -n 20

kubectl get pods -o yaml
kubectl get nodes -o yaml
exit 1
