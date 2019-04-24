#!/usr/bin/env bash
#script for deploy and setup kubernetes
#parameters:
# {1} path to folder with installation scripts, roles,... (usually templates/install)
# {2} url to OpenShift origin client
# {3} url to minikube
# {4} url to kubectl

SYSTEMTESTS_DIR=${1}
OPENSHIFT_CLIENT_URL=${2:-"https://github.com/openshift/origin/releases/download/v3.7.0/openshift-origin-client-tools-v3.7.0-7ed6862-linux-64bit.tar.gz"}
MINIKUBE_RELEASE_URL=${3:-"https://storage.googleapis.com/minikube/releases/v0.28.2/minikube-linux-amd64"}
KUBECTL_RELEASE_URL=${4:-"https://storage.googleapis.com/kubernetes-release/release/v1.9.4/bin/linux/amd64/kubectl"}
ansible-playbook ${SYSTEMTESTS_DIR}/ansible/playbooks/environment.yml \
    --extra-vars "{\"openshift_client_url\": \"${OPENSHIFT_CLIENT_URL}\", \"minikube_url\": \"${MINIKUBE_RELEASE_URL}\", \"kubectl_url\": \"${KUBECTL_RELEASE_URL}\"}" \
    -t openshift,kubectl,minikube

sudo sh -c 'sed -e 's/journald/json-file/g' -i /etc/docker/daemon.json'
sudo service docker restart && sleep 20

sudo snap install microk8s --classic
microk8s.enable dns storage registry

export KUBECONFIG=$HOME/.kube/config
