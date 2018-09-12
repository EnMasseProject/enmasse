#!/bin/bash
set -x
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

ENMASSE_DIR=$1
TESTCASE=$2
failure=0

export OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
export OPENSHIFT_USER=${OPENSHIFT_USER:-test}
export OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
export OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
export OPENSHIFT_TEST_LOGDIR=${OPENSHIFT_TEST_LOGDIR:-/tmp/testlogs}
export OPENSHIFT_USE_TLS=${OPENSHIFT_USE_TLS:-true}
export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
export DEFAULT_AUTHSERVICE=standard
export USE_MINIKUBE=true

SANITIZED_PROJECT=$OPENSHIFT_PROJECT
SANITIZED_PROJECT=${SANITIZED_PROJECT//_/-}
SANITIZED_PROJECT=${SANITIZED_PROJECT//\//-}
export OPENSHIFT_PROJECT=$SANITIZED_PROJECT

kubectl exec -ti busybox -- nslookup kubernetes.default

kubectl create namespace $OPENSHIFT_PROJECT
kubectl config set-context $(kubectl config current-context) --namespace=$OPENSHIFT_PROJECT


mkdir -p api-server-cert/
openssl req -new -x509 -batch -nodes -days 11000 -subj "/O=io.enmasse/CN=api-server.${OPENSHIFT_PROJET}.svc.cluster.local" -out api-server-cert/tls.crt -keyout api-server-cert/tls.key
kubectl create secret tls api-server-cert --cert=api-server-cert/tls.crt --key=api-server-cert/tls.key

mkdir -p none-authservice-cert/
openssl req -new -x509 -batch -nodes -days 11000 -subj "/O=io.enmasse/CN=none-authservice.${OPENSHIFT_PROJET}.svc.cluster.local" -out none-authservice-cert/tls.crt -keyout none-authservice-cert/tls.key
kubectl create secret tls none-authservice-cert --cert=none-authservice-cert/tls.crt --key=none-authservice-cert/tls.key

mkdir -p standard-authservice-cert/
openssl req -new -x509 -batch -nodes -days 11000 -subj "/O=io.enmasse/CN=standard-authservice.${OPENSHIFT_PROJET}.svc.cluster.local" -out standard-authservice-cert/tls.crt -keyout standard-authservice-cert/tls.key
kubectl create secret tls standard-authservice-cert --cert=standard-authservice-cert/tls.crt --key=standard-authservice-cert/tls.key

cp -r ${ENMASSE_DIR}/examples/install/none-authservice/*.yaml ${ENMASSE_DIR}/examples/bundles/enmasse-with-standard-authservice
sed -i "s/namespace: .*/namespace: ${OPENSHIFT_PROJECT}/" ${ENMASSE_DIR}/examples/bundles/enmasse-with-standard-authservice/*.yaml
kubectl create -f ${ENMASSE_DIR}/examples/bundles/enmasse-with-standard-authservice

#environment info
LOG_DIR="${ARTIFACTS_DIR}/kubernetes-info/"
mkdir -p ${LOG_DIR}
get_kubernetes_info ${LOG_DIR} services default "-before"
get_kubernetes_info ${LOG_DIR} pods default "-before"

#start docker logging
DOCKER_LOG_DIR="${ARTIFACTS_DIR}/docker-logs"
${CURDIR}/docker-logs.sh ${DOCKER_LOG_DIR} > /dev/null 2> /dev/null &
LOGS_PID=$!
echo "process for syncing docker logs is running with PID: ${LOGS_PID}"

#execute test
run_test ${TESTCASE} systemtests "kubernetes" || failure=$(($failure + 1))

kubectl get events --all-namespaces

#stop docker logging
echo "process for syncing docker logs with PID: ${LOGS_PID} will be killed"
kill ${LOGS_PID}
categorize_docker_logs "${DOCKER_LOG_DIR}" || true

#environment info
get_kubernetes_info ${LOG_DIR} pv ${OPENSHIFT_PROJECT}
get_kubernetes_info ${LOG_DIR} pods ${OPENSHIFT_PROJECT} 
get_kubernetes_info ${LOG_DIR} services default "-after"
get_kubernetes_info ${LOG_DIR} pods default "-after"
get_kubernetes_info ${LOG_DIR} events ${OPENSHIFT_PROJECT}

#store artifacts
$CURDIR/collect_logs.sh ${OPENSHIFT_TEST_LOGDIR} ${ARTIFACTS_DIR}

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
else
    teardown_test $OPENSHIFT_PROJECT
fi
