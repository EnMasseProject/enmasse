#!/usr/bin/env bash
#script for deploy and setup kubernetes
#parameters:
# {1} path to folder with installation scripts, roles,... (usually templates/install)
# {2} url to OpenShift origin client
# {3} url to minikube
# {4} url to kubectl

set -e

SYSTEMTESTS_DIR=${1}
OPENSHIFT_CLIENT_URL=${2:-"https://github.com/openshift/origin/releases/download/v3.7.0/openshift-origin-client-tools-v3.7.0-7ed6862-linux-64bit.tar.gz"}
MINIKUBE_RELEASE_URL=${3:-"https://storage.googleapis.com/minikube/releases/v0.33.1/minikube-linux-amd64"}
KUBECTL_RELEASE_URL=${4:-"https://storage.googleapis.com/kubernetes-release/release/v1.9.4/bin/linux/amd64/kubectl"}
ansible-playbook ${SYSTEMTESTS_DIR}/ansible/playbooks/environment.yml \
    --extra-vars "{\"openshift_client_url\": \"${OPENSHIFT_CLIENT_URL}\", \"minikube_url\": \"${MINIKUBE_RELEASE_URL}\", \"kubectl_url\": \"${KUBECTL_RELEASE_URL}\"}" \
    -t openshift,kubectl,minikube

export MINIKUBE_WANTUPDATENOTIFICATION=false
export MINIKUBE_WANTREPORTERRORPROMPT=false
export MINIKUBE_HOME=$HOME
export CHANGE_MINIKUBE_NONE_USER=true
mkdir $HOME/.kube || true
touch $HOME/.kube/config

sudo find /etc/docker

echo '{"log-driver":"json-file"}' | sudo sh -c 'cat - > /etc/docker/daemon.json'
sudo service docker restart && sleep 20

docker run -d -p 5000:5000 registry

sudo mount --make-rshared / # fix kube-dns issues
sudo minikube start --vm-driver=none --bootstrapper=kubeadm --kubernetes-version v1.9.4 --extra-config=apiserver.authorization-mode=RBAC --insecure-registry localhost:5000
sudo minikube config set WantUpdateNotification false
sudo minikube update-context
sudo minikube addons enable default-storageclass
kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=admin --serviceaccount=default:default

