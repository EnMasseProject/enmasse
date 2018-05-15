#!/bin/bash
function runcmd() {
    local cmd=$1
    local description=$2

    if [ "$GUIDE" == "true" ]; then
        echo "$description:"
        echo ""
        echo "...."
        echo "$cmd"
        echo "...."
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

function random_string() {
    head /dev/urandom | LC_CTYPE=C tr -dc A-Za-z0-9 | head -c 32
}

function create_address_space() {
  local CMD=$1
  local name=$2
  local namespace=$3

  payload="{ \\\"kind\\\":\\\"AddressSpace\\\", \\\"apiVersion\\\": \\\"enmasse.io/v1alpha1\\\", \\\"metadata\\\": { \\\"name\\\": \\\"$name\\\", \\\"namespace\\\": \\\"${namespace}\\\", \\\"annotations\\\": {\\\"enmasse.io/namespace\\\": \\\"$namespace\\\"} }, \\\"spec\\\": { \\\"type\\\": \\\"standard\\\", \\\"plan\\\": \\\"unlimited-standard\\\" } }"

  runcmd "cat <<EOF | $CMD create -n ${namespace} -f -
{
    \"apiVersion\": \"v1\",
    \"kind\": \"ConfigMap\",
    \"metadata\": {
        \"name\": \"${name}\",
        \"labels\": {
            \"type\": \"address-space\",
            \"namespace\": \"${namespace}\"
        }
    },
    \"data\": {
      \"config.json\": \"$payload\"
    }
}
EOF" "Create address space $name"
}

function create_csr() {
  local KEYFILE=$1
  local CSRFILE=$2
  local CN=${3::64}

  runcmd "openssl req -new -batch -nodes -keyout ${KEYFILE} -subj \"/O=io.enmasse/CN=${CN}\" -out ${CSRFILE}" "Create certificate signing request for ${CN}"
  fix_key_file_format ${KEYFILE}
}

function fix_key_file_format() {
  local KEYFILE=$1

  if grep -q 'BEGIN RSA PRIVATE KEY' ${KEYFILE}; then
      runcmd "mv ${KEYFILE} ${KEYFILE}.tmp" "Rename ${KEYFILE} so we can convert from PKCS#1 to PKCS#8"
      runcmd "openssl pkcs8 -topk8 -inform pem -in ${KEYFILE}.tmp -outform pem -nocrypt -out ${KEYFILE}" "Convert from PKCS#1 to PKCS#8"
      runcmd "rm ${KEYFILE}.tmp" "Delete the PKCS#1 format file" 
  fi
}

function sign_csr() {
  local CA_KEY=$1
  local CA_CERT=$2
  local CSRFILE=$3
  local CERTFILE=$4

  runcmd "openssl x509 -req -days 11000 -in ${CSRFILE} -CA ${CA_CERT} -CAkey ${CA_KEY} -CAcreateserial -out ${CERTFILE}" "Sign certificate with CA key"
}

function create_tls_secret() {
  local CMD=$1
  local SECRET_NAME=$2
  local KEYFILE=$3
  local CERTFILE=$4

  runcmd "$CMD create secret tls ${SECRET_NAME} -n ${NAMESPACE} --cert=${CERTFILE} --key=${KEYFILE}" "Create $SECRET_NAME TLS secret"
}

function create_and_sign_cert() {
    local CMD=$1
    local CA_KEY=$2
    local CA_CERT=$3
    local CN=${4::64}
    local SECRET_NAME=$5

    SERVER_KEY=${TEMPDIR}/${CN}.key
    SERVER_CERT=${TEMPDIR}/${CN}.crt
    SERVER_CSR=${TEMPDIR}/${CN}.csr

    create_csr $SERVER_KEY $SERVER_CSR $CN
    sign_csr $CA_KEY $CA_CERT $SERVER_CSR $SERVER_CERT
    create_tls_secret $CMD $SECRET_NAME $SERVER_KEY $SERVER_CERT
}

function create_self_signed_cert() {
    local CMD=$1
    local CN=${2::64}
    local SANS=""
    local LASTARG=$3
    shift 3
    while (( "$#" )); do

      if [[ "${SANS}" == "" ]]; then
	    SANS="subjectAltName=DNS:${LASTARG}"
      else
        SANS+=",DNS:${LASTARG}"
	  fi
	  LASTARG=$1

      shift

    done

    local SECRET_NAME=${LASTARG}


    KEY=${TEMPDIR}/${CN}.key
    CERT=${TEMPDIR}/${CN}.crt

    CSR=${TEMPDIR}/${CN}.csr
    CAKEY=${TEMPDIR}/${CN}.ca_key
    CACERT=${TEMPDIR}/${CN}.ca_crt

    if [[ "${SANS}" == "" ]]; then
    runcmd "openssl req -new -x509 -batch -nodes -days 11000 -out ${CERT} -keyout ${KEY} -subj \"/O=io.enmasse/CN=${CN}\"" "Create self-signed certificate for ${CN}"
    else
      runcmd "openssl genrsa -out ${CAKEY} 2048" "Create CA Key for ${CN}"
      runcmd "openssl req -new -x509 -days 1100 -key ${CAKEY} -subj \"/O=io.enmasse/CN=${CN}\" -out ${CACERT}" "Create CA Cert for ${CN}"
      runcmd "openssl req -newkey rsa:2048 -nodes -keyout ${KEY} -subj \"/O=io.enmasse/CN=${CN}\" -out ${CSR}" "Create CSR for ${CN}"
      runcmd "openssl x509 -req -extfile <(printf \"${SANS}\") -days 1100 -in ${CSR} -CA ${CACERT} -CAkey ${CAKEY} -CAcreateserial -out ${CERT}" "Create self-signed certificate for ${CN}"
    fi
    fix_key_file_format ${KEY}
    create_tls_secret $CMD $SECRET_NAME $KEY $CERT
}
