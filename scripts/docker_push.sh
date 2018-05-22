#!/usr/bin/env bash

curdir="$(dirname $(readlink -f ${0}))"
source "${curdir}/logger.sh"

if [[ -f Dockerfile ]]; then

    EVAL="${1}"
    RETRY=${2:-10} #repeat RETRY-times
    WAIT=${3:-10} #with WAIT seconds of sleep

    info "push docker images with retry mechanism"
    while (( RETRY > 0 )); do
        (( RETRY-- ))
        if ${EVAL}; then
            info "docker images successfully pushed"
            exit 0
        fi
        info "remaining retry operations: ${RETRY}"
        sleep ${WAIT}
    done
    err_and_exit "push dockerfile failed"
else
    warn "missing dockerfile!"
fi
