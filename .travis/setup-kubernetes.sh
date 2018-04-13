#!/bin/bash
#script for deploy and setup kubernetes
#parameters:
# {1} path to folder with installation scripts, roles,... (usually templates/install)
# {2} url to OpenShift origin client
# {3} url to minikube
# {4} url to kubectl

ENMASSE_DIR=${1}
OPENSHIFT_CLIENT_URL=${2:-"https://github.com/openshift/origin/releases/download/v3.7.0/openshift-origin-client-tools-v3.7.0-7ed6862-linux-64bit.tar.gz"}
MINIKUBE_RELEASE_URL=${3:-"https://storage.googleapis.com/minikube/releases/v0.25.2/minikube-linux-amd64"}
KUBECTL_RELEASE_URL=${4:-"https://storage.googleapis.com/kubernetes-release/release/v1.8.7/bin/linux/amd64/kubectl"}
ansible-playbook ${ENMASSE_DIR}/ansible/playbooks/openshift/environment.yml \
    --extra-vars "openshift_client_url=${OPENSHIFT_CLIENT_URL} minikube_url=${MINIKUBE_RELEASE_URL} kubectl_url=${KUBECTL_RELEASE_URL}" \
    -t openshift,kubectl,minikube

export MINIKUBE_WANTUPDATENOTIFICATION=false
export MINIKUBE_WANTREPORTERRORPROMPT=false
export MINIKUBE_HOME=$HOME
export CHANGE_MINIKUBE_NONE_USER=true
mkdir $HOME/.kube || true
touch $HOME/.kube/config

docker run -d -p 5000:5000 registry

export KUBECONFIG=$HOME/.kube/config
sudo -E minikube start --vm-driver=none --kubernetes-version v1.8.0 --insecure-registry localhost:5000
sudo -E minikube addons enable default-storageclass
