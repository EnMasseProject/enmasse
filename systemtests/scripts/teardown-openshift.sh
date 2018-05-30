#!/bin/bash
oc logout
oc cluster down
sudo rm -rf /var/lib/origin/openshift.local.pv
