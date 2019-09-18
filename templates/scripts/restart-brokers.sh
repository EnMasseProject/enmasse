#/bin/bash
ENMASSE_NAMESPACE=${1:-enmasse-infra}
MINREADY=${2:-0}
MINAVAILABLE=$(($MINREADY + 1))

for rset in `oc get statefulset -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE}`
do
    if [[ "$rset" == broker-* ]]; then
        ready=0
        while [[ "$ready" -lt "${MINAVAILABLE}" ]]
        do
            ready=`oc get statefulset ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}`
            if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
                sleep 1
            fi
        done

        infraUuid=`oc get statefulset ${rset} -o jsonpath='{.metadata.labels.infraUuid}'`

        echo "All broker pods in broker set ${rset} are ready. Initiating rolling restart."
        for rpod in `oc get pods -l role=broker,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}'`
        do
            if [[ "$rpod" == ${rset}* ]]; then
                echo "Deleting broker pod $rpod"
                oc delete pod $rpod
                sleep 30
                ready=0
                while [[ "$ready" -lt "${MINAVAILABLE}" ]]
                do
                    ready=`oc get statefulset ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}` || 0;
                    if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
                        echo "Waiting for minimum available broker replicas ${MINAVAILABLE} to be restored. Got $ready ready replicas."
                        sleep 10
                    fi
                done
                echo "Minimum ready broker replicas ${MINAVAILABLE} restored"
            fi
        done
    fi
done

for rset in `oc get deployment -l role=broker -o jsonpath='{.items[*].metadata.name}' -n ${ENMASSE_NAMESPACE}`
do
    if [[ "$rset" == broker* ]]; then
        ready=0
        while [[ "$ready" -lt "${MINAVAILABLE}" ]]
        do
            ready=`oc get deployment ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}`
            if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
                sleep 1
            fi
        done

        infraUuid=`oc get deployment ${rset} -o jsonpath='{.metadata.labels.infraUuid}'`

        echo "All broker pods in broker set ${rset} are ready. Initiating rolling restart."
        for rpod in `oc get pods -l role=broker,infraUuid=${infraUuid} -o jsonpath='{.items[*].metadata.name}'`
        do
            if [[ "$rpod" == ${rset}* ]]; then
                echo "Deleting broker pod $rpod"
                oc delete pod $rpod
                sleep 30
                ready=0
                while [[ "$ready" -lt "${MINAVAILABLE}" ]]
                do
                    ready=`oc get deployment ${rset} -o jsonpath='{.status.readyReplicas}' -n ${ENMASSE_NAMESPACE}` || 0;
                    if [[ "$ready" -lt "${MINAVAILABLE}" ]]; then
                        echo "Waiting for minimum available broker replicas ${MINAVAILABLE} to be restored. Got $ready ready replicas."
                        sleep 10
                    fi
                done
                echo "Minimum ready broker replicas ${MINAVAILABLE} restored"
            fi
        done
    fi
done
echo "Broker restart complete!"
