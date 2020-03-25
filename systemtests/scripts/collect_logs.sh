#!/bin/bash
LOGDIR=$1
ARTIFACTS_DIR=$2

if which oc &> /dev/null; then
    CMD=oc
elif which kubectl &> /dev/null; then
    CMD=kubectl
else
    >&2 echo "$0: Cannot find oc or kubectl command, please check path to ensure it is installed"
    exit 1
fi


function runcmd {
    local cmd=$1
    local logfile=$2
    echo "$cmd > $logfile"
    ${cmd} > ${logfile}
}

mkdir -p ${ARTIFACTS_DIR}/logs

for pod in `${CMD} get pods -o jsonpath='{.items[*].metadata.name}'`
do
    for container in `${CMD} get pod $pod -o jsonpath='{.spec.containers[*].name}'`
    do
        runcmd "${CMD} logs -c $container $pod" ${ARTIFACTS_DIR}/logs/${pod}_${container}.log
        if [[ "$container" == "router" ]]; then
            runcmd "${CMD} rsh -c $container $pod python /usr/bin/qdmanage query --type=address" ${ARTIFACTS_DIR}/logs/${pod}_${container}_router_address.txt
            runcmd "${CMD} rsh -c $container $pod python /usr/bin/qdmanage query --type=connection"  ${ARTIFACTS_DIR}/logs/${pod}_${container}_router_connection.txt
            runcmd "${CMD} rsh -c $container $pod python /usr/bin/qdmanage query --type=connector" ${ARTIFACTS_DIR}/logs/${pod}_${container}_router_connector.txt
        fi
    done
done

if [[ ! -z "$(ls ${LOGDIR})" ]]; then
    cp -r ${LOGDIR}/* ${ARTIFACTS_DIR}/logs/
    tar -czvf ${ARTIFACTS_DIR}/components-jmap.tar.gz $(find ${ARTIFACTS_DIR} -name *.bin) || true
    rm -rf $(find ${ARTIFACTS_DIR} -name *.bin) || true
fi
