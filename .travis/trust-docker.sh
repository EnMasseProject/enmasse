#!/bin/sh
sudo cat /etc/default/docker
sudo service docker stop
sudo sh -c 'echo INSECURE_REGISTRY=\"--insecure-registry 172.30.0.0/16\" >> /etc/default/docker'
sudo cat /etc/default/docker
sudo service docker start
