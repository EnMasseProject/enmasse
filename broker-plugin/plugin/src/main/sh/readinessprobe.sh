#!/bin/sh
user=$(cat /opt/apache-artemis/support/username)
password=$(cat /opt/apache-artemis/support/password)

if [ "${user}" == "" ]; then
    user=admin
fi

if [ "${password}" == "" ]; then
    password=admin
fi

ARTEMIS_INSTANCE="${HOME}/${AMQ_NAME}"
found=$(${ARTEMIS_INSTANCE}/bin/artemis queue stat --field NAME --operation EQUALS --value ${PROBE_ADDRESS} --user ${user} --password ${password} | grep -c ${PROBE_ADDRESS})
if [ $found -gt 0 ]; then
    exit 0
fi
exit 1
