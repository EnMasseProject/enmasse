#!/usr/bin/env bash
if [[ -f Dockerfile ]]; then

    EVAL="${1}"
    RETRY=${2:-10} #repeat RETRY-times
    WAIT=${3:-10} #with WAIT seconds of sleep

    echo "INFO: push docker images with retry mechanism"
    while [[ ${RETRY} > 0 ]]; do
        (( RETRY-- ))
        ${EVAL}
        if [[ $? = 0 ]]; then
            exit 0
        fi
        echo "INFO: remaining retry operations: ${RETRY}"
        sleep ${WAIT}
    done
    echo "ERROR: push dockerfile failed"
    exit 1
else
    echo "WARN: missing dockerfile!"
fi
