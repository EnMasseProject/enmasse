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
KEYCLOAK_TEMPLATE=$SCRIPTDIR/openshift/addons/keycloak.yaml
KEYCLOAK_TEMPLATE_PARAMS=""
TEMPLATE_NAME=enmasse
TEMPLATE_PARAMS=""
KEYCLOAK_PASSWORD=`head /dev/urandom | tr -dc A-Za-z0-9 | head -c 128`

DEFAULT_USER=developer
DEFAULT_NAMESPACE=myproject
OC_ARGS=""
GUIDE=false

while getopts dgk:m:n:p:st:u:yvh opt; do
    case $opt in
        d)
            OS_ALLINONE=true
            ;;
        g)
            GUIDE=true
            ;;
        k)
            KEYCLOAK_PASSWORD=$OPTARG
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
            SERVICE_CATALOG=true
            OC_SERVICE_CATALOG_FLAG="--service-catalog"
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
            echo "  -h                   show this help message"
            echo "  -d                   create an all-in-one docker OpenShift on localhost"
            echo "  -k KEYCLOAK_PASSWORD The password to use as the keycloak admin user"
            echo "  -n NAMESPACE         OpenShift project name to install EnMasse into (default: $DEFAULT_NAMESPACE)"
            echo "  -m MASTER            OpenShift master URI to login against (default: https://localhost:8443)"
            echo "  -p PARAMS            Custom template parameters to pass to EnMasse template ('cat $SCRIPTDIR/openshift/enmasse.yaml' to get a list of available parameters)"
            echo "  -s                   Experimental: Only applicable when also using -d option. Starts OpenShift with Service Catalog enabled and registers EnMasse Service Broker"
            echo "  -t TEMPLATE          An alternative OpenShift template file to deploy EnMasse"
            echo "  -u USER              OpenShift user to run commands as (default: $DEFAULT_USER)"
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
    runcmd "oc cluster up $OC_SERVICE_CATALOG_FLAG" "Start local OpenShift cluster"
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
runcmd "oc create secret generic keycloak-credentials --from-literal=admin.username=admin --from-literal=admin.password=$KEYCLOAK_PASSWORD" "Create secret with keycloak admin credentials"

if [[ $TEMPLATE_PARAMS == *"MULTIINSTANCE=true"* ]]; then
    if [ -n "$OS_ALLINONE" ]
    then
        runcmd "oc login -u system:admin" "Logging in as system:admin"
        runcmd "oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:${NAMESPACE}:enmasse-service-account" "Granting cluster-admin rights to enmasse-service-account"
        runcmd "oc login -u $OS_USER $OC_ARGS $MASTER_URI" "Login as $OS_USER"
    else
        echo "Please grant cluster-admin rights to system:serviceaccount:${NAMESPACE}:enmasse-service-account before creating instances: 'oc adm policy add-cluster-role-to-user cluster-admin system:serviceaccount:${NAMESPACE}:enmasse-service-account'"
    fi
fi

if [ -n "$ALT_TEMPLATE" ]
then
    ENMASSE_TEMPLATE=$ALT_TEMPLATE
fi

runcmd "oc process -f $KEYCLOAK_TEMPLATE $KEYCLOAK_TEMPLATE_PARAMS | oc create -n $NAMESPACE -f -" "Instantiate keycloak template"
runcmd "oc process -f $ENMASSE_TEMPLATE $TEMPLATE_PARAMS | oc create -n $NAMESPACE -f -" "Instantiate EnMasse template"

if [ -n "$OS_ALLINONE" ] && [ -n "$SERVICE_CATALOG" ]
then
    runcmd "oc login -u system:admin" "Logging in as system:admin"
    runcmd "oc create secret generic enmasse-broker-auth --from-literal=username=foo --from-literal=password=bar -n service-catalog" "Creating Secret with EnMasse Service Broker auth credentials"
    runcmd "cat <<EOF | oc create -f -
apiVersion: servicecatalog.k8s.io/v1alpha1
kind: Broker
metadata:
  name: enmasse
spec:
  url: http://address-controller.${NAMESPACE}.svc.cluster.local:8080
  authInfo:
    basicAuthSecret:
      namespace: service-catalog
      name: enmasse-broker-auth
EOF" "Registering EnMasse Service Broker in Service Catalog"

#    runcmd "oc login -u $OS_USER $OC_ARGS $MASTER_URI" "Login as $OS_USER"
fi
