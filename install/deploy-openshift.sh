#!/bin/bash

# This script is for deploying EnMasse into OpenShift. The target of
# installation can be an existing OpenShift deployment or an all-in-one
# container can be started.
#
# In either case, access to the `oc` command is required.
#
# example usage:
#
#    $ enmasse-deploy.sh -c 10.0.1.100 -o enmasse.10.0.1.100.xip.io
#
# this will deploy EnMasse into the OpenShift cluster running at 10.0.1.100
# and set the EnMasse webui route url to enmasse.10.0.1.100.xip.io.
# further it will use the user `developer` and project `myproject`, asking
# for a login when appropriate.
# for further parameters please see the help text.

if which oc &> /dev/null
then :
else
    echo "Cannot find oc command, please check path to ensure it is installed"
    exit 1
fi

SCRIPTDIR=`dirname $0`
ENMASSE_TEMPLATE=$SCRIPTDIR/openshift/enmasse.yaml
TEMPLATE_NAME=enmasse
TEMPLATE_PARAMS=""

DEFAULT_USER=developer
DEFAULT_NAMESPACE=myproject
OC_ARGS=""
GUIDE=false

while getopts dgk:m:n:p:s:t:u:yvh opt; do
    case $opt in
        d)
            OS_ALLINONE=true
            ;;
        g)
            GUIDE=true
            ;;
        k)
            SERVER_KEY=$OPTARG
            ;;
        m)
            MASTER_URI=$OPTARG
            ;;
        n)
            NAMESPACE=$OPTARG
            ;;
        p)
            TEMPLATE_PARAMS="$OPTARG $TEMPLATE_PARAMS"
            ;;
        s)
            SERVER_CERT=$OPTARG
            ;;
        t)
            ALT_TEMPLATE=$OPTARG
            ;;
        u)
            OS_USER=$OPTARG
            USER_REQUESTED=true
            ;;
        y)
            OC_ARGS="--insecure-skip-tls-verify=true"
            ;;
        v)
            set -x
            ;;
        h)
            echo "usage: deploy-openshift.sh [options]"
            echo
            echo "deploy the EnMasse suite into a running OpenShift cluster"
            echo
            echo "optional arguments:"
            echo "  -h             show this help message"
            echo "  -d             create an all-in-one docker OpenShift on localhost"
            echo "  -k KEY         Address controller private key (default: self-signed)"
            echo "  -n NAMESPACE   OpenShift project name to install EnMasse into (default: $DEFAULT_NAMESPACE)"
            echo "  -m MASTER      OpenShift master URI to login against (default: https://localhost:8443)"
            echo "  -p PARAMS      Custom template parameters to pass to EnMasse template ('cat $SCRIPTDIR/openshift/enmasse.yaml' to get a list of available parameters)"
            echo "  -s CERT        Address controller certificate (default: self-signed)"
            echo "  -t TEMPLATE    An alternative OpenShift template file to deploy EnMasse"
            echo "  -u USER        OpenShift user to run commands as (default: $DEFAULT_USER)"
            echo
            exit
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            exit
            ;;
    esac
done

source $SCRIPTDIR/common.sh
TEMPDIR=`tempdir`

if [ -z "$OS_USER" ]
then
    OS_USER=$DEFAULT_USER
fi

if [ -z "$NAMESPACE" ]
then
    NAMESPACE=$DEFAULT_NAMESPACE
fi

if [ -n "$OS_ALLINONE" ]
then
    if [ -n "$MASTER_URI" ]
    then
        echo "Error: You have requested an all-in-one deployment AND specified a cluster address."
        echo "Please choose one of these options and restart."
        exit 1
    fi
    if [ -n "$USER_REQUESTED" ]
    then
        echo "Error: You have requested an all-in-one deployment AND specified an OpenShift user."
        echo "Please choose either all-in-one or a cluster deployment if you need to use a specific user."
        exit 1
    fi
    runcmd "oc cluster up" "Start local OpenShift cluster"
fi


runcmd "oc login -u $OS_USER $OC_ARGS $MASTER_URI" "Login as $OS_USER"

AVAILABLE_PROJECTS=`docmd "oc projects -q"`

for proj in $AVAILABLE_PROJECTS
do
    if [ "$proj" == "$NAMESPACE" ]; then
        runcmd "oc project $proj" "Select project"
        break
    fi
done

CURRENT_PROJECT=`docmd "oc project -q"`
if [ "$CURRENT_PROJECT" != "$NAMESPACE" ]; then
    runcmd "oc new-project $NAMESPACE" "Create new project $NAMESPACE"
fi

runcmd "oc create sa enmasse-service-account -n $NAMESPACE" "Create service account for address controller"
runcmd "oc policy add-role-to-user view system:serviceaccount:${NAMESPACE}:default" "Add permissions for viewing OpenShift resources to default user"
runcmd "oc policy add-role-to-user edit system:serviceaccount:${NAMESPACE}:enmasse-service-account" "Add permissions for editing OpenShift resources to EnMasse service account"

if [[ $TEMPLATE_PARAMS == *"MULTIINSTANCE=true"* ]]; then
    echo "Please grant cluster-admin rights to system:serviceaccount:${NAMESPACE}:enmasse-service-account before creating instances: 'oadm policy add-cluster-role-to-user cluster-admin system:serviceaccount:${NAMESPACE}:enmasse-service-account'"
fi

if [ -n "$ALT_TEMPLATE" ]
then
    ENMASSE_TEMPLATE=$ALT_TEMPLATE
fi

if [ -z "$SERVER_KEY" ] || [ -z "$SERVER_CERT" ]; then
    SERVER_KEY=${TEMPDIR}/enmasse-controller.key
    SERVER_CERT=${TEMPDIR}/enmasse-controller.crt
    runcmd "oc adm ca create-signer-cert --key=${TEMPDIR}/ca.key --cert=${TEMPDIR}/ca.crt --serial=${TEMPDIR}/ca.serial.txt --name=enmasse-signer@\$(date +%s)" "Create signer CA"
    runcmd "oc adm ca create-server-cert --key=${TEMPDIR}/enmasse-controller-pkcs1.key --cert=${SERVER_CERT} --hostnames=address-controller.${NAMESPACE}.svc.cluster.local --signer-cert=${TEMPDIR}/ca.crt --signer-key=${TEMPDIR}/ca.key --signer-serial=${TEMPDIR}/ca.serial.txt" "Create signed server certificate for address-controller"
    runcmd "openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in ${TEMPDIR}/enmasse-controller-pkcs1.key -out ${SERVER_KEY}" "Convert key to correct PKCS#8 format"
fi

runcmd "oc secret new enmasse-controller-certs tls.crt=${SERVER_CERT} tls.key=${SERVER_KEY}" "Create secret for controller certificate"
runcmd "oc secret add serviceaccount/enmasse-service-account secrets/enmasse-controller-certs --for=mount" "Add controller secret mount permissions for enmasse-service-account"

runcmd "oc process -f $ENMASSE_TEMPLATE $TEMPLATE_PARAMS | oc create -n $NAMESPACE -f -" "Instantiate EnMasse template"
