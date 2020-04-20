#!/bin/bash
set -e

#Docker installation from google default repositories
sudo apt-get -y update

sudo apt-get install -y apt-transport-https ca-certificates curl gnupg-agent software-properties-common
sudo mkdir -p /mnt/docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -

sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"

sudo apt-get -y update
sudo apt-get install docker-ce docker-ce-cli containerd.io

#Configuring docker to github actions disk

sudo systemctl stop docker
sudo sh -c "sed -i 's#ExecStart=/usr/bin/dockerd -H fd://#ExecStart=/usr/bin/dockerd -g /mnt/docker -H fd://#' /lib/systemd/system/docker.service"
sudo systemctl daemon-reload
sudo rsync -aqxP /var/lib/docker/ /mnt/docker


#Starting docker
sudo systemctl start docker

df -h