#!/bin/bash

CURDIR="$(dirname $(readlink -f ${0}))"

ansible-playbook ${CURDIR}/../ansible/playbooks/environment.yml -t openshift
