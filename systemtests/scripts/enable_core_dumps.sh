#!/usr/bin/env bash
CORE_DUMPS_LOCATION=${1}

ulimit -c unlimited
mkdir -p ${CORE_DUMPS_LOCATION}
chmod a+rwx ${CORE_DUMPS_LOCATION}
echo "${CORE_DUMPS_LOCATION}/core.%e.%p.%h.%t" > /proc/sys/kernel/core_pattern

echo "foo bar baz" > "${CORE_DUMPS_LOCATION}/core.test"
firefox --display=${DISPLAY} &
pid_fire=$!
kill -s 11 ${pid_fire}
sleep 5
