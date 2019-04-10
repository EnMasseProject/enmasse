#!/usr/bin/env bash

set -e

get_endpoint() {
   key=$1
   out=$(python -c "import sys, json;  print(json.load(sys.stdin)['$key'])")
   echo ${out}
}

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TARGET_DIR=${1-/apps}

export COOKIE_SECRET=$(python -c 'import os,base64; print base64.b64encode(os.urandom(16))')

WELLKNOWN_DIR=${TARGET_DIR}/.well-known
mkdir -p ${WELLKNOWN_DIR}
OAUTH_AUTH_SERVER=${WELLKNOWN_DIR}/oauth-authorization-server

if [[ "${DISCOVERY_METADATA_URL}" =~ ^data: ]];
then
   echo -n "${DISCOVERY_METADATA_URL}" | sed -e 's/^data:.*,//' | base64 --decode > ${OAUTH_AUTH_SERVER}
   AUTHORIZATION_ENDPOINT=$(get_endpoint authorization_endpoint < ${OAUTH_AUTH_SERVER})
   TOKEN_ENDPOINT=$(get_endpoint token_endpoint < ${OAUTH_AUTH_SERVER})
   ISSUER=$(get_endpoint issuer < ${OAUTH_AUTH_SERVER})
   # OpenShift OAUTH2 server doesn't support a userinfo endpoint.
   OPENSHIFT_VALIDATE_ENDPOINT="${ISSUER}/apis/user.openshift.io/v1/users/~"
else
   curl --insecure "${DISCOVERY_METADATA_URL}" > ${OAUTH_AUTH_SERVER}
   ISSUER=$(get_endpoint issuer < ${OAUTH_AUTH_SERVER})
fi

find ${SCRIPT_DIR}/.. -type d
mkdir -p ${TARGET_DIR}
tar -cf - -C ${SCRIPT_DIR}/.. . | tar -xf - --no-overwrite-dir -C ${TARGET_DIR}

for c in $(find ${TARGET_DIR} -name "*.cfg" -type f)
do
  sed -e "s,\${ISSUER},${ISSUER},g" \
      -e "s,\${AUTHORIZATION_ENDPOINT},${AUTHORIZATION_ENDPOINT},g" \
      -e "s,\${TOKEN_ENDPOINT},${TOKEN_ENDPOINT},g" \
      -e "s,\${OPENSHIFT_VALIDATE_ENDPOINT},${OPENSHIFT_VALIDATE_ENDPOINT},g" \
      -e "s,\${COOKIE_SECRET},${COOKIE_SECRET},g" \
      -e "s,\${OAUTH2_SCOPE},${OAUTH2_SCOPE},g" \
      -i ${c}
done
