#!/usr/bin/env bash

set -e

PROFILE=${1:-"systemtests"}
TESTCASE=${2}

if [[ -n "$TESTCASE" ]]; then
    EXTRA_ARGS="-Dtest=$TESTCASE"
fi

cd .. && mvn test -pl systemtests -P${PROFILE} -am -Djava.net.preferIPv4Stack=true -Djansi.force=true -DfailIfNoTests=false -Dstyle.color=always -DskipTests ${EXTRA_ARGS} -DtrimStackTrace=false --no-transfer-progress
cd systemtests
