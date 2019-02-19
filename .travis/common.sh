#!/bin/bash
function use_external_registry() {
    if [[ "${BRANCH}" == "master" ]] || [[ "${BRANCH}" == "${VERSION}"* ]] && [[ "${PULL_REQUEST}" == "false" ]]
    then
        return 0
    else
        return 1
    fi
}
