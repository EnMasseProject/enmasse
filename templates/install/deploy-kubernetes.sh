#!/bin/bash

# This script is for deploying EnMasse into Kubernetes. The target of
# installation can be an existing Kubernetes deployment or an all-in-one
# container can be started.
#
# In either case, access to the `kubectl` command is required.
#
# example usage:
#
#    $ deploy-kubernetes.sh -c 10.0.1.100 -l
#
# this will deploy EnMasse into the Kubernetes master running at 10.0.1.100
# and apply the external load balancer support for Azure, AWS etc.  For further
# parameters please see the help text.

if which kubectl &> /dev/null
then :
else
    echo "Cannot find kubectl command, please check path to ensure it is installed"
    exit 1
fi

SCRIPTDIR=`dirname $0`
TEMPLATE_PARAMS=""
ADDONS=$SCRIPTDIR/kubernetes/addons
ENMASSE_TEMPLATE=$SCRIPTDIR/kubernetes/enmasse.yaml
KEYCLOAK_TEMPLATE=$ADDONS/standard-authservice.yaml
NONE_TEMPLATE=$ADDONS/none-authservice.yaml
DEFAULT_NAMESPACE=enmasse
AUTH_SERVICES="none"
GUIDE=false
MODE="singletenant"

while getopts a:dglm:n:o:t:vh opt; do
    case $opt in
        a)
            AUTH_SERVICES=$OPTARG
            ;;
        d)
            ALLINONE=true
            ;;
        g)
            GUIDE=true
            ;;
        l)
            EXTERNAL_LB=true
            ;;
        m)
            MASTER_URI=$OPTARG
            ;;
        n)
            NAMESPACE=$OPTARG
            ;;
        o)
            MODE=$OPTARG
            ;;
        t)
            ALT_TEMPLATE=$OPTARG
            ;;
        v)
            set -x
            ;;
        h)
            echo "usage: deploy-kubernetes.sh [options]"
            echo
            echo "deploy the EnMasse suite into a running Kubernetes cluster"
            echo
            echo "optional arguments:"
            echo "  -h                   show this help message"
            echo "  -a \"none standard\" Deploy given authentication services (default: \"none\")"
            echo "  -d                   create an all-in-one minikube VM on localhost"
            echo "  -m MASTER            Kubernetes master URI to login against (default: https://localhost:8443)"
            echo "  -n NAMESPACE         Namespace to deploy EnMasse into (default: $DEFAULT_NAMESPACE)"
            echo "  -o mode              Deploy in given mode, 'singletenant' or 'multitenant'.  (default: \"singletenant\")"
            echo "  -t TEMPLATE          An alternative Kubernetes template file to deploy EnMasse"
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

if [ -z "$NAMESPACE" ]
then
    NAMESPACE=$DEFAULT_NAMESPACE
fi

if [ -n "$ALLINONE" ]
then
    if [ -n "$MASTER_URI" ]
    then
        echo "Error: You have requested an all-in-one deployment AND specified a cluster address."
        echo "Please choose one of these options and restart."
        exit 1
    fi
    runcmd "minikube start" "Start local minikube cluster"
    runcmd "minikube addons enable ingress" "Enabling ingress controller"
fi


e=`kubectl get namespace ${NAMESPACE} 2> /dev/null`
if [ $? -gt 0 ]; then
    runcmd "kubectl create namespace $NAMESPACE" "Create namespace $NAMESPACE"
fi

runcmd "kubectl create sa enmasse-admin -n $NAMESPACE" "Create service account for address controller"

create_self_signed_cert "kubectl" "address-controller.${NAMESPACE}.svc.cluster.local" "address-controller-cert"

for auth_service in $AUTH_SERVICES
do
    if [ "$auth_service" == "none" ]; then
        create_self_signed_cert "kubectl" "none-authservice.${NAMESPACE}.svc.cluster.local" "none-authservice-cert"
        runcmd "kubectl apply -f $NONE_TEMPLATE -n $NAMESPACE" "Create none authservice"
    fi
    if [ "$auth_service" == "standard" ]; then
        KEYCLOAK_PASSWORD=`random_string`
        create_self_signed_cert "kubectl" "standard-authservice.${NAMESPACE}.svc.cluster.local" "standard-authservice-cert"
        runcmd "kubectl create secret generic keycloak-credentials --from-literal=admin.username=admin --from-literal=admin.password=$KEYCLOAK_PASSWORD -n $NAMESPACE" "Create secret with keycloak admin credentials"
        runcmd "kubectl apply -f $KEYCLOAK_TEMPLATE -n $NAMESPACE" "Create standard authservice"
    fi
done

if [ -n "$ALT_TEMPLATE" ]
then
    ENMASSE_TEMPLATE=$ALT_TEMPLATE
fi

if [ "$MODE" == "singletenant" ]; then
    runcmd "kubectl apply -f $ADDONS/resource-definitions.yaml -n $NAMESPACE" "Create resource definitions"
    runcmd "kubectl apply -f $ADDONS/standard-plans.yaml -n $NAMESPACE" "Create standard address space plans"
    runcmd "kubectl create sa address-space-admin -n $NAMESPACE" "Create service account for default address space"
    create_address_space "kubectl" "default" $NAMESPACE
else
    runcmd "kubectl apply -f $ADDONS/resource-definitions.yaml -n $NAMESPACE" "Create resource definitions"
    runcmd "kubectl apply -f $ADDONS/standard-plans.yaml -n $NAMESPACE" "Create standard address space plans"
    runcmd "kubectl apply -f $ADDONS/brokered-plans.yaml -n $NAMESPACE" "Create brokered address space plans"
fi

runcmd "kubectl apply -f $ENMASSE_TEMPLATE -n $NAMESPACE" "Deploy EnMasse to $NAMESPACE"

if [ "$EXTERNAL_LB" == "true" ]
then
    runcmd "kubectl apply -f kubernetes/addons/external-lb.yaml -n $NAMESPACE"
fi
