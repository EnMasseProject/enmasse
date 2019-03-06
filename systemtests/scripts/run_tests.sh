#!/bin/sh
TESTCASE=$1
PROFILE=${2:-"systemtests"}

if [[ -n "$TESTCASE" ]]; then
    EXTRA_ARGS="-Dtest=$TESTCASE"
fi

cd .. && mvn test -pl systemtests -P${PROFILE} -am -Djava.net.preferIPv4Stack=true -Djansi.force=true -DfailIfNoTests=false -Dstyle.color=always ${EXTRA_ARGS}
