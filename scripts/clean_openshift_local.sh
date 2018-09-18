#!/usr/bin/env bash
# Clean-up the OpenShift work directory that is created by oc cluster up.
# Should be run after oc cluster down.

TARGET=${1:-openshift.local.clusterup}

if [[ ${EUID} -ne 0 ]]; then
   >&2 echo "$0: This script must be run as root"
   exit 1
fi


if [ -d "${TARGET}" ]; then
   echo Cleaning OpenShift work directory ${TARGET}
   find ${TARGET} -type d -exec mountpoint --quiet {} \; -exec umount --types tmpfs {} \;
   rm -rf ${TARGET}
else
   >&2 echo "$0: ${TARGET} not found"
   exit -1
fi



