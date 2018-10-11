#!/usr/bin/env bash

if [[ ! -z "${_NODE_OPTIONS}" ]]; then
    set -- ${_NODE_OPTIONS} "${@}"
fi

node "${@}"
