#!/bin/bash
function runcmd() {
    local cmd=$1
    local description=$2

    if [ "$GUIDE" == "true" ]; then
        echo "$description:"
        echo ""
        echo "    $cmd"
        echo ""
    else
        bash -c "$cmd"
    fi
}

function docmd() {
    local cmd=$1
    if [ -z $GUIDE ] || [ "$GUIDE" == "false" ]; then
        $cmd
    fi
}

function tempdir() {
    echo `mktemp -d /tmp/enmasse-deploy.XXXXXX`
}

function create_csr() {
  local KEYFILE=$1
  local CSRFILE=$2
  local CN=$3

  runcmd "openssl req -new -batch -nodes -keyout ${KEYFILE} -subj \"/O=io.enmasse/CN=${CN}\" -out ${CSRFILE}" "Create certificate signing request for ${CN}"
}

function sign_csr() {
  local CA_KEY=$1
  local CA_CERT=$2
  local CSRFILE=$3
  local CERTFILE=$4

  runcmd "openssl x509 -req -days 11000 -in ${CSRFILE} -CA ${CA_CERT} -CAkey ${CA_KEY} -CAcreateserial -out ${CERTFILE}" "Sign address-controller certificate with CA key"
}

function create_tls_secret() {
  local CMD=$1
  local SECRET_NAME=$2
  local KEYFILE=$3
  local CERTFILE=$4

  runcmd "cat <<EOF | $CMD create -n ${NAMESPACE} -f -
{
    \"apiVersion\": \"v1\",
    \"kind\": \"Secret\",
    \"metadata\": {
        \"name\": \"$SECRET_NAME\"
    },
    \"type\": \"kubernetes.io/tls\",
    \"data\": {
        \"tls.key\": \"\$(base64 -w 0 ${KEYFILE})\",
        \"tls.crt\": \"\$(base64 -w 0 ${CERTFILE})\"
    }
}
EOF" "Create $SECRET_NAME TLS secret"
}

function create_and_sign_cert() {
    local CMD=$1
    local CA_KEY=$2
    local CA_CERT=$3
    local CN=$4
    local SECRET_NAME=$5

    SERVER_KEY=${TEMPDIR}/${CN}.key
    SERVER_CERT=${TEMPDIR}/${CN}.crt
    SERVER_CSR=${TEMPDIR}/${CN}.csr

    create_csr $SERVER_KEY $SERVER_CSR $CN
    sign_csr $CA_KEY $CA_CERT $SERVER_CSR $SERVER_CERT
    create_tls_secret $CMD $SECRET_NAME $SERVER_KEY $SERVER_CERT
}
