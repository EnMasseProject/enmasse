#!/bin/bash

echo "Update archives"

sudo add-apt-repository ppa:longsleep/golang-backports
sudo apt-get update

echo "Install maven"

sudo apt install maven

echo "Install nodeJS"

sudo apt-get install curl
curl -sL https://deb.nodesource.com/setup_13.x
sudo -E bash -
sudo apt-get install nodejs

echo "Install golang"
sudo apt-get install golang-1.13

echo "Install asciidoctor"
gem install asciidoctor

echo "Clean cache"
sudo apt-get clean
