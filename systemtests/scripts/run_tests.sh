#!/bin/sh
TESTCASE=$1

if [ -n "$TESTCASE" ]; then
    EXTRA_ARGS="-Dtest.single=$TESTCASE"
fi

gradle :systemtests:test -Psystemtests -i --rerun-tasks -Djava.net.preferIPv4Stack=true $EXTRA_ARGS
