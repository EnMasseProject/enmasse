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
            echo "  -k KEY         Server key file (default: none)"
            echo "  -n NAMESPACE   OpenShift project name to install EnMasse into (default: $DEFAULT_NAMESPACE)"
            echo "  -m MASTER      OpenShift master URI to login against (default: https://localhost:8443)"
            echo "  -p PARAMS      Custom template parameters to pass to EnMasse template"
            echo "  -s CERT        Server certificate file (default: none)"
            echo "  -t TEMPLATE    An alternative opan OpenShift template file to deploy EnMasse"
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

if [ -z "$MULTIINSTANCE" ] && [ -n "$SERVER_KEY" ] && [ -n "$SERVER_CERT" ]
then
    runcmd "oc secret new ${NAMESPACE}-certs ${SERVER_CERT} ${SERVER_KEY}" "Create certificate secret"
    runcmd "oc secret add serviceaccount/default secrets/${NAMESPACE}-certs --for=mount" "Add certificate secret to default service account"
    TEMPLATE_PARAMS="INSTANCE_CERT_SECRET=${NAMESPACE}-certs ${TEMPLATE_PARAMS}"
fi

if [ -n "$ALT_TEMPLATE" ]
then
    ENMASSE_TEMPLATE=$ALT_TEMPLATE
fi

runcmd "oc process -f $ENMASSE_TEMPLATE $TEMPLATE_PARAMS | oc create -n $NAMESPACE -f -" "Instantiate EnMasse template"
