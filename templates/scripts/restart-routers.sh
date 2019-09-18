#/bin/bash
ENMASSE_NAMESPACE=${1:-amq-online-infra}
MINREADY=${2:-1}
MINAVAILABLE=$(($MINREADY + 1))
echo "MIN AVAILABLE: $MINAVAILABLE"
for rset in `oc get statefulset -l name=qdrouterd -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE}`
do
    ready=0
    while [[ "$ready" -lt "${MINAVAILABLE}" ]]
    do
        ready=`oc get statefulset ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}`
        if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
            sleep 1
        fi
    done

    infraUuid=`oc get statefulset ${rset} -o jsonpath='{.metadata.labels.infraUuid}'`
    
    echo "All pods in router set ${rset} are ready. Initiating rolling restart."
    for rpod in `oc get pods -l capability=router,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}'`
    do
        echo "Restarting router $rpod"
        oc delete pod $rpod
        sleep 30
        echo "Waiting for minimum ready router replicas ${MINREADY} to be restored"
        ready=0
        while [[ "$ready" -lt "${MINAVAILABLE}" ]]
        do
            ready=`oc get statefulset ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}`
            if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
                sleep 1
            fi
        done
    done
done
