#!/bin/sh
TESTCASE=$1
PROFILE=${2:-"systemtests"}

if [[ -n "$TESTCASE" ]]; then
    EXTRA_ARGS="-Dtest=$TESTCASE"
fi

mvn test -P${PROFILE} -Djava.net.preferIPv4Stack=true -Djansi.force=true -Dstyle.color=always ${EXTRA_ARGS}
