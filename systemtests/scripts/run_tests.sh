#!/bin/sh
TESTCASE=$1

if [ -n "$TESTCASE" ]; then
    EXTRA_ARGS="-Dtest=$TESTCASE"
fi

mvn test -pl systemtests -Psystemtests -Djava.net.preferIPv4Stack=true $EXTRA_ARGS
