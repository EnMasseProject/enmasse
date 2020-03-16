## Bulk insert

    TENANT=enmasse-infra.iot2
    for i in $(seq -w 100); do
      hat -t $TENANT device create device-$i
      hat -t $TENANT creds set-password device-$i auth-$i $(uuid)
    done