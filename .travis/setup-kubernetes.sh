#!/usr/bin/env bash

sudo sh -c 'sed -e 's/journald/json-file/g' -i /etc/docker/daemon.json'
sudo service docker restart && sleep 20

sudo snap install microk8s --classic
microk8s.enable storage registry

export KUBECONFIG=$HOME/.kube/config
