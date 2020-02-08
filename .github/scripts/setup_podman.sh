#!/bin/bash
sudo apt update

sudo apt -y  install software-properties-common
sudo add-apt-repository -y ppa:projectatomic/ppa

sudo apt update
sudo apt -y install podman