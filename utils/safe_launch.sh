#!/bin/bash

trap "" TERM INT

CMD=$1
shift

exec $CMD "$@"

