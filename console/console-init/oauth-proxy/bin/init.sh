#!/usr/bin/env bash
#
# Copyright 2019, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

set -e

get_endpoint() {
   key=$1
   out=$(python -c "import sys, json;  print(json.load(sys.stdin)['$key'])")
   echo ${out}
}

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
TARGET_DIR=${1-/apps}

SSO_COOKIE_SECRET=$(python -c \
'import os,base64; \
 print(base64.urlsafe_b64encode(bytes(os.environ["SSO_COOKIE_SECRET"], "utf-8") if "SSO_COOKIE_SECRET" in os.environ and os.environ["SSO_COOKIE_SECRET"] else os.urandom(32)).decode())')

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
      -e "s,\${SSO_COOKIE_SECRET},${SSO_COOKIE_SECRET},g" \
      -e "s,\${SSO_COOKIE_DOMAIN},${SSO_COOKIE_DOMAIN},g" \
      -e "s,\${OAUTH2_SCOPE},${OAUTH2_SCOPE},g" \
      -i ${c}
done

echo "window.env = {" > ${TARGET_DIR}/www/env.js
echo "  OPENSHIFT_AVAILABLE:${OPENSHIFT_AVAILABLE}," >> ${TARGET_DIR}/www/env.js
if [ ! -z ${ITEM_REFRESH_RATE} ]; then
  echo "  ITEM_REFRESH_RATE:${ITEM_REFRESH_RATE}," >> ${TARGET_DIR}/www/env.js
fi
echo "};" >> ${TARGET_DIR}/www/env.js
