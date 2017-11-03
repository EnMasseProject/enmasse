oc cluster up $OC_CLUSTER_ARGS
oc login -u system:admin
oc cluster status
while [ $? -gt 0 ]
do
    sleep 5
    oc cluster status
done

sleep 30
