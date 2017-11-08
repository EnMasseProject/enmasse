#!/usr/bin/env bash
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

TCK_PATH=$1
CLI_ID=$2
JMS_VERSION=$3
JMS_CLIENT=$4
JMS_BROKER=$5

#install prerequisites
sudo yum -y install patch

#setup environment
curl -X POST -H "content-type: application/json" --data-binary @./systemtests/templates/tckAddressSpace.json http://$(oc get route -o jsonpath='{.spec.host}' restapi)/v1/addressspaces
wait_until_up 2 'tck-brokered'
curl -X PUT -H "content-type: application/json" --data-binary @./systemtests/templates/tckAddresses.json http://$(oc get route -o jsonpath='{.spec.host}' restapi)/v1/addresses/tck-brokered
sleep 40

#keycloak user
oc extract secret/keycloak-credentials
USER=$(cat admin.username)
PASSWORD=$(cat admin.password)

# get token
RESULT=$(curl --data "grant_type=password&client_id=${CLI_ID}&username=${USER}&password=${PASSWORD}" http://$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/realms/master/protocol/openid-connect/token)
TOKEN=`echo ${RESULT} | sed 's/.*access_token":"//g' | sed 's/".*//g'`

#create user (tckuser/tckuser)
curl -X POST -H "content-type: application/json" --data-binary @./systemtests/templates/tckUser.json -H "Authorization: Bearer $TOKEN"  http://${USER}:${PASSWORD}@$(oc get routes -o jsonpath='{.spec.host}' keycloak)/auth/admin/realms/tck-brokered/users

#run tck
host=$(oc get route -n tck-brokered -o jsonpath='{.spec.host}' messaging)
port=443
cd ${TCK_PATH}
./gradlew -PjmsVersion=${JMS_VERSION} -PjmsClient=${JMS_CLIENT} -PjmsBroker=${JMS_BROKER} -Pupstream -Phost=${host} -Pport=${port} runTck
