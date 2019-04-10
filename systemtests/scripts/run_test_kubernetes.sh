#!/bin/bash
set -xe
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

ENMASSE_DIR=$1
TEST_PROFILE=$2
TESTCASE=$3
failure=0

: ${KUBE_OPERATION:=create}

BASE_DIR="${CURDIR}/../../"
API_URL=$(kubectl config view --minify | grep server | cut -f 2- -d ":" | tr -d " ")
API_TOKEN=$(kubectl describe secret $(kubectl get serviceaccount default -o jsonpath='{.secrets[0].name}') | grep -E '^token' | cut -f2 -d':' | tr -d " ")

export KUBERNETES_API_URL=${KUBERNETES_API_URL:-${API_URL}}
export KUBERNETES_API_TOKEN=${KUBERNETES_API_TOKEN:-${API_TOKEN}}
export KUBERNETES_NAMESPACE=${KUBERNETES_NAMESPACE:-enmasseci}
export TEST_LOGDIR=${TEST_LOGDIR:-/tmp/testlogs}
export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
export DEFAULT_AUTHSERVICE=standard
export USE_MINIKUBE=true

SANITIZED_NAMESPACE=${KUBERNETES_NAMESPACE}
SANITIZED_NAMESPACE=${SANITIZED_NAMESPACE//_/-}
SANITIZED_NAMESPACE=${SANITIZED_NAMESPACE//\//-}
export KUBERNETES_NAMESPACE=${SANITIZED_NAMESPACE}

kubectl create namespace ${KUBERNETES_NAMESPACE}
kubectl config set-context $(kubectl config current-context) --namespace=${KUBERNETES_NAMESPACE}

mkdir -p api-server-cert/
openssl req -new -x509 -batch -nodes -days 11000 -subj "/O=io.enmasse/CN=api-server.${KUBERNETES_NAMESPACE}.svc.cluster.local" -out api-server-cert/tls.crt -keyout api-server-cert/tls.key
kubectl create secret tls api-server-cert --cert=api-server-cert/tls.crt --key=api-server-cert/tls.key

sed -i "s/enmasse-infra/${KUBERNETES_NAMESPACE}/" ${ENMASSE_DIR}/install/*/*/*.yaml
kubectl ${KUBE_OPERATION} -f ${ENMASSE_DIR}/install/bundles/enmasse
kubectl ${KUBE_OPERATION} -f ${ENMASSE_DIR}/install/components/example-plans
kubectl ${KUBE_OPERATION} -f ${ENMASSE_DIR}/install/components/example-roles
cat <<EOF | kubectl create -f -
apiVersion: admin.enmasse.io/v1beta1
kind: AuthenticationService
metadata:
  name: standard-authservice
spec:
  type: standard
  standard:
    resources:
      requests:
        memory: 1Gi
      limits:
        memory: 1Gi
EOF

cat <<EOF | kubectl create -f -
apiVersion: admin.enmasse.io/v1beta1
kind: AuthenticationService
metadata:
  name: none-authservice
spec:
  type: none
  none:
    resources:
      requests:
        memory: 128Mi
      limits:
        memory: 128Mi
EOF


if [[ "${DEPLOY_IOT}" == "true" ]]; then
    echo "Deploying IoT components"
    sed -i "s/enmasse-infra/${KUBERNETES_NAMESPACE}/" ${ENMASSE_DIR}/install/*/*/*/*.yaml

    NAMESPACE="${KUBERNETES_NAMESPACE}" "${BASE_DIR}/iot/examples/k8s-tls/create"
    NAMESPACE="${KUBERNETES_NAMESPACE}" PREFIX="systemtests-" "${BASE_DIR}/iot/examples/k8s-tls/deploy"

    kubectl ${KUBE_OPERATION} -f ${ENMASSE_DIR}/install/components/iot/api
    kubectl ${KUBE_OPERATION} -f ${ENMASSE_DIR}/install/components/iot/common
    kubectl ${KUBE_OPERATION} -f ${ENMASSE_DIR}/install/components/iot/operator
else
    echo "Not deploying IoT components"
fi

#environment info
LOG_DIR="${ARTIFACTS_DIR}/kubernetes-info/"
mkdir -p ${LOG_DIR}
get_kubernetes_info ${LOG_DIR} services default "-before"
get_kubernetes_info ${LOG_DIR} pods default "-before"

if [[ -z "${DISABLE_LOG_SYNC}" ]]; then
    #start docker logging
    DOCKER_LOG_DIR="${ARTIFACTS_DIR}/pod-logs"
    ${CURDIR}/pod-logs.sh ${DOCKER_LOG_DIR} ${KUBERNETES_NAMESPACE} > /dev/null 2> /dev/null &
    LOGS_PID=$!
    echo "process for syncing docker logs is running with PID: ${LOGS_PID}"
fi

wait_until_enmasse_up 'kubernetes' ${KUBERNETES_NAMESPACE}

echo "Running test profile: ${TEST_PROFILE}"
#execute test
case "${TEST_PROFILE}" in
"smoke")
    run_test shared-brokered "**.SmokeTest" || failure=$(($failure + 1))
    run_test shared-standard "**.SmokeTest" || failure=$(($failure + 1))
    ;;
"smoke-iot")
    run_test smoke-iot "" || failure=$(($failure + 1))
    ;;
*)
    run_test systemtests ${TESTCASE} || failure=$(($failure + 1))
    ;;
esac

kubectl get events --all-namespaces --sort-by lastTimestamp

if [[ -z "${DISABLE_LOG_SYNC}" ]]; then
    #stop docker logging
    echo "process for syncing docker logs with PID: ${LOGS_PID} will be killed"
    kill ${LOGS_PID} || true
fi

#environment info
get_kubernetes_info ${LOG_DIR} pv ${KUBERNETES_NAMESPACE}
get_kubernetes_info ${LOG_DIR} pods ${KUBERNETES_NAMESPACE} 
get_kubernetes_info ${LOG_DIR} services default "-after"
get_kubernetes_info ${LOG_DIR} pods default "-after"
get_kubernetes_info ${LOG_DIR} events ${KUBERNETES_NAMESPACE}
print_images

#store artifacts
${CURDIR}/collect_logs.sh ${TEST_LOGDIR} ${ARTIFACTS_DIR}

if [[ ${failure} -gt 0 ]]; then
    echo "Systemtests failed"
    exit 1
else
    teardown_test ${KUBERNETES_NAMESPACE} kubectl
fi
echo "End of run_test_kubernetes.sh"
