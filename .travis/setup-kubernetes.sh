#!/usr/bin/env bash
#script for deploy and setup kubernetes
#parameters:
# {1} path to folder with installation scripts, roles,... (usually templates/install)
# {2} url to OpenShift origin client
# {3} url to minikube
# {4} url to kubectl

SYSTEMTESTS_DIR=${1}
OPENSHIFT_CLIENT_URL=${2}
MINIKUBE_RELEASE_URL=${3}
KUBECTL_RELEASE_URL=${4}
ansible-playbook ${SYSTEMTESTS_DIR}/ansible/playbooks/environment.yml \
    --extra-vars "{\"openshift_client_url\": \"${OPENSHIFT_CLIENT_URL}\", \"minikube_url\": \"${MINIKUBE_RELEASE_URL}\", \"kubectl_url\": \"${KUBECTL_RELEASE_URL}\"}" \
    -t openshift,kubectl,minikube

export MINIKUBE_WANTUPDATENOTIFICATION=false
export MINIKUBE_WANTREPORTERRORPROMPT=false
export MINIKUBE_HOME=$HOME
export CHANGE_MINIKUBE_NONE_USER=true
mkdir $HOME/.kube || true
touch $HOME/.kube/config

sudo sh -c 'sed -e 's/journald/json-file/g' -i /etc/docker/daemon.json'
sudo service docker restart && sleep 20

docker run -d -p 5000:5000 registry

export KUBECONFIG=$HOME/.kube/config
sudo -E minikube start --vm-driver=none --insecure-registry localhost:5000
minikube config set WantUpdateNotification false
minikube update-context
