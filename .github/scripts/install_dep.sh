#!/bin/bash

echo "Update archives"
sudo apt-get update

echo "Install maven"

sudo apt install maven

echo "Clean cache"
sudo apt-get clean
