#!/usr/bin/env bash

set -e

SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

PROFILE=${1:-"systemtests"}
TESTCASE=${2}

if [[ -n "$TESTCASE" ]]; then
    EXTRA_ARGS="${EXTRA_ARGS} -Dtest=${TESTCASE}"
fi

if [[ "$MAVEN_DEBUG" == "true" ]]; then
    EXTRA_ARGS="${EXTRA_ARGS} -X -e"
fi

pushd ${SCRIPTDIR}/../.. && mvn test -pl systemtests -P${PROFILE} -am -Djava.net.preferIPv4Stack=true -Djansi.force=true -DfailIfNoTests=true -Dstyle.color=always -DskipTests ${EXTRA_ARGS} -DtrimStackTrace=false
popd
