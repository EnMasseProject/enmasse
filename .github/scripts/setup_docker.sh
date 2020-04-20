#!/bin/bash
set -e

#Remove github thrash

echo "=============================================================================="
echo "Freeing up disk space on CI system"
echo "=============================================================================="
sudo rm -rf "/usr/local/share/boost"
sudo rm -rf "$AGENT_TOOLSDIRECTORY"
echo "Listing 100 largest packages"
dpkg-query -Wf '${Installed-Size}\t${Package}\n' | sort -n | tail -n 100
df -h
echo "Removing large packages"
sudo apt-get remove -y '^ghc-8.*'
sudo apt-get remove -y '^dotnet-.*'
sudo apt-get remove -y '^llvm-.*'
sudo apt-get remove -y 'php.*'
sudo apt-get remove -y azure-cli google-cloud-sdk hhvm google-chrome-stable firefox powershell mono-devel
sudo apt-get autoremove -y
sudo apt-get clean -y
df -h
echo "Removing large directories"
# deleting 15GB
rm -rf /usr/share/dotnet/
df -h

sudo apt-get -y clean

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
sudo sh -c "sed -i 's#ExecStart=/usr/bin/dockerd -H fd://#ExecStart=/usr/bin/dockerd -H fd://#' /lib/systemd/system/docker.service"
sudo systemctl daemon-reload
sudo rsync -aqxP /var/lib/docker/ /mnt/docker


#Starting docker
sudo systemctl start docker

df -h