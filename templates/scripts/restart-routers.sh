#/bin/bash
ENMASSE_NAMESPACE=${1:-enmasse-infra}
MINREADY=${2:-1}
MINAVAILABLE=$(($MINREADY + 1))
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
        echo "Deleting router pod $rpod"
        oc delete pod $rpod
        sleep 30
        ready=0
        while [[ "$ready" -lt "${MINAVAILABLE}" ]]
        do
            ready=`oc get statefulset ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}`
            if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
                echo "Waiting for minimum available router replicas ${MINAVAILABLE} to be restored. Got ${ready} ready replicas."
                sleep 10
            fi
        done
        echo "Minimum ready router replicas ${MINAVAILABLE} restored"
    done
done
echo "Router restart complete!"
