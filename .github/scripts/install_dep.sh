#!/bin/bash
echo "Install maven"

sudo apt install maven

echo "Install nodeJS"

sudo apt-get install curl
curl -sL https://deb.nodesource.com/setup_13.x
sudo -E bash -
sudo apt-get install nodejs


echo "Install golang"
    
sudo add-apt-repository ppa:longsleep/golang-backports
sudo apt-get update
sudo apt-get install golang-1.12


echo "Install asciidoctor"
gem install asciidoctor

sudo apt-get clean