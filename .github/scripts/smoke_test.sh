#!/bin/bash
#set -e

TAG=${TAG:-latest}
export TEMPLATES=${PWD}/templates/build/enmasse-${TAG}

#echo "Running smoke tests"
#time make PROFILE=smoke systemtests

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

sudo docker system df
sudo docker system info

kubectl get pods -o yaml
kubectl get nodes -o yaml
exit 1