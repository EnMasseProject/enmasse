#!/usr/bin/env bash
#script for deploy and setup kubernetes
function install_kubectl {
    if [[ "${TEST_KUBECTL_VERSION:-latest}" = "latest" ]]; then
        TEST_KUBECTL_VERSION=$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)
    fi
    curl -Lo kubectl https://storage.googleapis.com/kubernetes-release/release/${TEST_KUBECTL_VERSION}/bin/linux/amd64/kubectl && chmod +x kubectl
    sudo cp kubectl /usr/bin
}

function wait_for_minikube {
    i="0"

    while [[ $i -lt 60 ]]
    do
        # The role needs to be added because Minikube is not fully prepared for RBAC.
        # Without adding the cluster-admin rights to the default service account in kube-system
        # some components would be crashing (such as KubeDNS). This should have no impact on
        # RBAC for Strimzi during the system tests.
        kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=cluster-admin --serviceaccount=kube-system:default
        if [[ $? -ne 0 ]]
        then
            sleep 1
        else
            return 0
        fi
        i=$[$i+1]
    done

    return 1
}

install_kubectl
if [[ "${TEST_MINIKUBE_VERSION:-latest}" = "latest" ]]; then
    TEST_MINIKUBE_URL=https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
else
    TEST_MINIKUBE_URL=https://github.com/kubernetes/minikube/releases/download/${TEST_MINIKUBE_VERSION}/minikube-linux-amd64
fi
curl -Lo minikube ${TEST_MINIKUBE_URL} && chmod +x minikube
sudo cp minikube /usr/bin

export MINIKUBE_WANTUPDATENOTIFICATION=false
export MINIKUBE_WANTREPORTERRORPROMPT=false
export MINIKUBE_HOME=$HOME
export CHANGE_MINIKUBE_NONE_USER=true

mkdir $HOME/.kube || true
touch $HOME/.kube/config

docker run -d -p 5000:5000 registry

export KUBECONFIG=$HOME/.kube/config
sudo -E minikube start --vm-driver=none --kubernetes-version=v1.15.0 \
  --insecure-registry=localhost:5000 --extra-config=apiserver.authorization-mode=RBAC
sudo chown -R travis: /home/travis/.minikube/
sudo -E minikube addons enable default-storageclass

wait_for_minikube

if [[ $? -ne 0 ]]
then
    echo "Minikube failed to start or RBAC could not be properly set up"
    exit 1
fi
