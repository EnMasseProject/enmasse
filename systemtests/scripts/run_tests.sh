#!/usr/bin/env bash

set -e

PROFILE=${1:-"systemtests"}
TEST=${2}

if [[ -n "$TESTCASE" ]]; then
    EXTRA_ARGS="-Dtest=${TEST}"
fi

cd .. && mvn test -pl systemtests -P"${PROFILE}" -am -Djava.net.preferIPv4Stack=true -Djansi.force=true -DfailIfNoTests=false -Dstyle.color=always -DskipTests "${EXTRA_ARGS}" -DtrimStackTrace=false
cd systemtests
