#!/usr/bin/env bash

# Ensure that we have a suitable node interpreter.  This ought to be declared declaratively in the package.json, within an engines section,
# but we currently don't install the package so this wouldn't be enforced.
REQUIRED_NODE_MAJOR=6
NODE_MAJOR=$(node -p 'process.version.match("^v?(\\d+)\.")[1]')
if [[ ${NODE_MAJOR} -lt ${REQUIRED_NODE_MAJOR} ]]; then
    2>&1 echo "Node major version ${NODE_MAJOR} [$(node --version)] too low - require ${REQUIRED_NODE_MAJOR}"
    exit 1
fi

if [[ ! -z "${_NODE_OPTIONS}" ]]; then
    set -- ${_NODE_OPTIONS} "${@}"
fi

node "${@}"
