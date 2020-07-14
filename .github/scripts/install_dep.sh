#!/bin/bash

echo "Update archives"

sudo add-apt-repository ppa:longsleep/golang-backports
sudo apt-get update

echo "Install maven"

sudo apt install maven

echo "Install nodeJS"

sudo apt-get install curl
curl -sL https://deb.nodesource.com/setup_12.x | sudo -E bash -
sudo apt-get install -y nodejs yarn

echo "Clean cache"
sudo apt-get clean
