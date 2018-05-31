#!/bin/bash

# This script is for deploying EnMasse into OpenShift or Kubernetes. 
# The target of installation can be an existing OpenShift deployment 
# or an all-in-one container can be started.
#
# In either case, access to the `oc` or `kubectl` command is required.
#
# example usage:
#
#    $ deploy.sh -n enmasse -a standard
#
# this will deploy EnMasse into the OpenShift cluster with authentication services.
# Further it will use the user `developer` and project `enmasse`, asking
# for a login when appropriate.
# for further parameters please see the help text.
SCRIPTDIR=`dirname $0`
source $SCRIPTDIR/common.sh
TEMPDIR=`tempdir`
RESOURCE_DIR=${SCRIPTDIR}/resources
TEMPLATE_NAME=enmasse
TEMPLATE_PARAMS=""
AUTH_SERVICES="none"

CLUSTER_TYPE=openshift
DEFAULT_USER=developer
DEFAULT_NAMESPACE=myproject
OC_ARGS=""
GUIDE=false
MODE="singletenant"

while getopts a:c:de:gk:m:n:o:u:t:yvh opt; do
    case $opt in
        a)
            AUTH_SERVICES=$OPTARG
            ;;
        c)
            CERT_FILE=$OPTARG
            ;;
        d)
            OS_ALLINONE=true
            ;;
        e)
            ENVIRONMENT=$OPTARG
            ;;
        g)
            GUIDE=true
            ;;
        k)
            KEY_FILE=$OPTARG
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
        u)
            KUBE_USER=$OPTARG
            USER_REQUESTED=true
            ;;
        t)
            CLUSTER_TYPE=$OPTARG
            ;;
        y)
            OC_ARGS="--insecure-skip-tls-verify=true"
            ;;
        v)
            set -x
            ;;
        *)
            echo "usage: deploy.sh [options]"
            echo
            echo "deploy the EnMasse suite into a running Kubernetes cluster"
            echo
            echo "optional arguments:"
            echo "  -h                   show this help message"
            echo "  -a TYPE              Deploy one or more given authentication services (default: \"none\")"
            echo "                         TYPE := { none | standard }[,TYPE]"
            echo "  -c FILE              Provide a PEM encoded certificate file of the full certificate chain"
            echo "                         Must be used in combination with -k"
            echo "  -d                   create an all-in-one cluster on localhost"
            echo "  -e                   Environment label for this EnMasse deployment"
            echo "  -k FILE              Provide a PEM encoded key file used for external connections"
            echo "                         Must be used in combination with -c"
            echo "  -m MASTER            Master URI to login against (default: https://localhost:8443)"
            echo "  -n NAMESPACE         Project name to install EnMasse into (default: $DEFAULT_NAMESPACE)"
            echo "  -o MODE              Deploy in given tenant mode.  (default: \"singletenant\")"
            echo "                         MODE := { singletenant | multitenant }"
            echo "  -u USER              User to run commands as (default: $DEFAULT_USER)"
            echo "  -t CLUSTER_TYPE      Type of cluster. (default: openshift)"
            echo "                         CLUSTER_TYPE := { openshift | kubernetes }"
            echo
            exit
            ;;
    esac
done

if [ -n "$KEY_FILE" -a -n "$CERT_FILE" ]; then
    test -r "$CERT_FILE" || die "Certificate file '$CERT_FILE' cannot be found or read"
    test -r "$KEY_FILE" || die "Key file '$KEY_FILE' cannot be found or read"
    CERT_PROVIDED=true
elif [ -n "$KEY_FILE" -o -n "$CERT_FILE" ]; then
    die "The arguments -c and -k must both be used in combination"
fi

if [ "$CLUSTER_TYPE" == "openshift" ];
then
    USE_OPENSHIFT=true
    CMD=oc
else
    CMD=kubectl
fi

if [ -n "$USE_OPENSHIFT" ]; then
    if which oc &> /dev/null
    then
        :
    else
        echo "Cannot find 'oc' command, please check path to ensure it is installed"
        exit 1
    fi
elif which kubectl &> /dev/null
then
    :
else
    echo "Cannot find 'kubectl' command, please check path to ensure it is installed"
    exit 1
fi

if [ -z "$KUBE_USER" ]
then
    KUBE_USER=$DEFAULT_USER
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
        echo "Error: You have requested an all-in-one deployment AND specified a user."
        echo "Please choose either all-in-one or a cluster deployment if you need to use a specific user."
        exit 1
    fi
    runcmd "oc cluster up $OC_SERVICE_CATALOG_FLAG" "Start local OpenShift cluster"
fi


if [ -n "$USE_OPENSHIFT" ]; then
    runcmd "oc login -u $KUBE_USER $OC_ARGS $MASTER_URI" "Login as $KUBE_USER"
    rc=$?;
    if [[ $rc != 0 ]]; then
        exit $rc;
    fi
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
else
    e=`kubectl get namespace ${NAMESPACE} 2> /dev/null`
    if [ $? -gt 0 ]; then
        runcmd "kubectl create namespace $NAMESPACE" "Create namespace $NAMESPACE"
    fi
fi

runcmd "$CMD create sa enmasse-admin -n $NAMESPACE" "Create service account for address space controller"
runcmd "$CMD label -n ${NAMESPACE} sa enmasse-admin app=enmasse"

if [ -n "$USE_OPENSHIFT" ]; then
    runcmd "oc policy add-role-to-user view system:serviceaccount:${NAMESPACE}:default --rolebinding-name=enmasse-view" "Add permissions for viewing OpenShift resources to default user"
    runcmd "oc policy add-role-to-user admin system:serviceaccount:${NAMESPACE}:enmasse-admin --rolebinding-name=enmasse-admin" "Add permissions for editing OpenShift resources to admin SA"

    runcmd "oc label -n ${NAMESPACE} rolebinding enmasse-view app=enmasse"
    runcmd "oc label -n ${NAMESPACE} rolebinding enmasse-admin app=enmasse"
fi

for auth_service in ${AUTH_SERVICES//,/ }
do
    if [ "$auth_service" == "none" ]; then
        create_self_signed_cert "$CMD" "none-authservice.${NAMESPACE}.svc.cluster.local" "none-authservice-cert"
        runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/none-authservice/deployment.yaml" "Create none authservice deployment"
        runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/none-authservice/service.yaml" "Create none authservice service"
    fi
    if [ "$auth_service" == "standard" ]; then
        KEYCLOAK_PASSWORD=`random_string`
        create_self_signed_cert "$CMD" "standard-authservice.${NAMESPACE}.svc.cluster.local" "standard-authservice-cert"
        runcmd "$CMD create -n ${NAMESPACE} secret generic keycloak-credentials --from-literal=admin.username=admin --from-literal=admin.password=$KEYCLOAK_PASSWORD" "Create secret with keycloak admin credentials"
        runcmd "$CMD label -n ${NAMESPACE} secret keycloak-credentials app=enmasse"
        runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/standard-authservice/service.yaml" "Create standard authservice service"
        if [ -n "$USE_OPENSHIFT" ]; then
            runcmd "oc create -n ${NAMESPACE} -f ${RESOURCE_DIR}/standard-authservice/route.yaml" "Create standard authservice route"
            OAUTH_DISABLED="false"
            HTTP_URL="https://$($CMD get service standard-authservice -n ${NAMESPACE} -o jsonpath={.spec.clusterIP}):8443/auth"
        else
            runcmd "kubectl create -n ${NAMESPACE} -f ${RESOURCE_DIR}/standard-authservice/external-service.yaml" "Create standard authservice external service"
            OAUTH_DISABLED="true"
            HTTP_URL="https://$($CMD get service standard-authservice -n ${NAMESPACE} -o jsonpath={.spec.clusterIP}):8443/auth"
        fi
        runcmd "$CMD create -n ${NAMESPACE} configmap keycloak-config --from-literal=hostname=standard-authservice.${NAMESPACE}.svc --from-literal=oauthDisabled=${OAUTH_DISABLED} --from-literal=httpUrl=${HTTP_URL} --from-literal=port=5671 --from-literal=caSecretName=standard-authservice-cert" "Create standard authentication service configuration"
	runcmd "$CMD label -n ${NAMESPACE} configmap keycloak-config app=enmasse"
        runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/standard-authservice/pvc.yaml" "Create standard authservice persistent volume"
        runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/standard-authservice/keycloak-deployment.yaml" "Create standard authservice deployment"
        runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/standard-authservice/controller-deployment.yaml" "Create standard authservice controller"
    fi
done

if [ "$MODE" == "singletenant" ]; then
    runcmd "$CMD create -n ${NAMESPACE} -f $RESOURCE_DIR/resource-definitions/resource-definitions.yaml" "Create resource definitions"
    runcmd "$CMD create -n ${NAMESPACE} -f $RESOURCE_DIR/plans/standard-plans.yaml" "Create standard address space plans"
    runcmd "$CMD create sa address-space-admin -n $NAMESPACE" "Create service account for default address space"
    runcmd "$CMD label -n ${NAMESPACE} sa address-space-admin app=enmasse"
    if [ -n "$USE_OPENSHIFT" ]; then
        runcmd "oc policy add-role-to-user admin system:serviceaccount:${NAMESPACE}:address-space-admin --rolebinding-name=enmasse-addressspace-admin" "Add permissions for editing OpenShift resources to address space admin SA"
        runcmd "oc label -n ${NAMESPACE} rolebinding enmasse-addressspace-admin app=enmasse"
    fi

    create_address_space "$CMD" "default" $NAMESPACE
elif [ $MODE == "multitenant" ]; then
    runcmd "$CMD create -n ${NAMESPACE} -f $RESOURCE_DIR/resource-definitions/resource-definitions.yaml" "Create resource definitions"
    runcmd "$CMD create -n ${NAMESPACE} -f $RESOURCE_DIR/plans/standard-plans.yaml" "Create standard address space plans"
    runcmd "$CMD create -n ${NAMESPACE} -f $RESOURCE_DIR/plans/brokered-plans.yaml" "Create brokered address space plans"
    if [ "$USE_OPENSHIFT" == "true" ]; then
        runcmd "$CMD create -n ${NAMESPACE} configmap api-server-config --from-literal=enableRbac=false" "Create api-server configmap"
	runcmd "$CMD label -n ${NAMESPACE} configmap api-server-config app=enmasse"
    fi
else
    echo "Unknown deployment mode $MODE"
    exit 1
fi

if [ "$CERT_PROVIDED" == "true" ]; then
    runcmd "$CMD create -n ${NAMESPACE} configmap address-space-controller-config --from-literal=wildcardEndpointCertSecret=api-server-cert" "Create address-space-controller configmap"
else
    runcmd "$CMD create -n ${NAMESPACE} configmap address-space-controller-config" "Create address-space-controller configmap"
fi
runcmd "$CMD label -n ${NAMESPACE} configmap address-space-controller-config app=enmasse"
runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/address-space-controller/address-space-definitions.yaml" "Create address space definitions"
runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/address-space-controller/deployment.yaml" "Create address space controller deployment"

if [ "$CERT_PROVIDED" == "true" ]; then
    create_tls_secret $CMD api-server-cert "$KEY_FILE" "$CERT_FILE"
else
    create_self_signed_cert "$CMD" "api-server.${NAMESPACE}.svc.cluster.local" "api-server-cert"
fi

runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/api-server/deployment.yaml" "Create api server deployment"
runcmd "$CMD create -n ${NAMESPACE} -f ${RESOURCE_DIR}/api-server/service.yaml" "Create api server service"

if [ "$USE_OPENSHIFT" == "true" ]; then
    runcmd "oc create -n ${NAMESPACE} -f ${RESOURCE_DIR}/api-server/route.yaml" "Create restapi route"
else
    runcmd "kubectl expose service api-server -n ${NAMESPACE} --port=443 --target-port=8443 --name=restapi --type=LoadBalancer" "Create external restapi service"
fi

if [ $MODE == "multitenant" ]; then
    if [ -n "$OS_ALLINONE" ] && [ -n "$USE_OPENSHIFT" ]
    then
        runcmd "oc login -u system:admin" "Logging in as system:admin"
        runcmd "oc create -f ${RESOURCE_DIR}/cluster-roles/address-space-controller.yaml -n $NAMESPACE" "Create cluster roles needed for address-space-controller"
        runcmd "oc create -f ${RESOURCE_DIR}/cluster-roles/api-server.yaml -n $NAMESPACE" "Create cluster roles needed for multitenant api server"
        runcmd "oc adm policy add-cluster-role-to-user enmasse.io:address-space-controller system:serviceaccount:${NAMESPACE}:enmasse-admin" "Granting address-space-controller rights to enmasse-admin"
        runcmd "oc adm policy add-cluster-role-to-user enmasse.io:api-server system:serviceaccount:${NAMESPACE}:enmasse-admin" "Granting api-server rights to enmasse-admin"
        runcmd "oc adm policy add-cluster-role-to-user enmasse.io:keycloak-controller system:serviceaccount:${NAMESPACE}:enmasse-admin" "Granting keycloak-controller rights to enmasse-admin"
        runcmd "oc adm policy add-cluster-role-to-user system:auth-delegator system:serviceaccount:${NAMESPACE}:enmasse-admin" "Granting auth-delegator rights to enmasse-admin"
        runcmd "oc login -u $KUBE_USER $OC_ARGS $MASTER_URI" "Login as $KUBE_USER"
    elif [ -n "$USE_OPENSHIFT" ]; then
        echo "Please create cluster roles required to run EnMasse with RBAC: 'oc create -f ${RESOURCE_DIR}/cluster-roles/'"
        echo "Please add enmasse.io:address-space-controller to system:serviceaccount:${NAMESPACE}:enmasse-admin before creating instances: 'oc adm policy add-cluster-role-to-user enmasse.io:address-space-controller system:serviceaccount:${NAMESPACE}:enmasse-admin'"
        echo "Please add enmasse.io:api-server to system:serviceaccount:${NAMESPACE}:enmasse-admin before creating instances: 'oc adm policy add-cluster-role-to-user enmasse.io:api-server system:serviceaccount:${NAMESPACE}:enmasse-admin'"
        echo "Please add enmasse.io:keycloak-controller to system:serviceaccount:${NAMESPACE}:enmasse-admin before creating instances: 'oc adm policy add-cluster-role-to-user enmasse.io:keycloak-controller system:serviceaccount:${NAMESPACE}:enmasse-admin'"
        echo "Please add system:auth-delegator to system:serviceaccount:${NAMESPACE}:enmasse-admin before creating instances: 'oc adm policy add-cluster-role-to-user system:auth-delegator system:serviceaccount:${NAMESPACE}:enmasse-admin'"
    fi
fi
