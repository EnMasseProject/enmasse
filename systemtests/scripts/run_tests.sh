#!/bin/sh
gradle :systemtests:test -Psystemtests -i --rerun-tasks -Djava.net.preferIPv4Stack=true $1
