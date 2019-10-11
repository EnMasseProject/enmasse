#!/bin/bash

if [[ "${PULL_REQUEST}" == "true" ]]; then
	export IMAGE_PULL_POLICY=IfNotPresent
fi

function use_external_registry() {
    if [[ "${BRANCH}" == "master" ]] || [[ "${BRANCH}" == "${VERSION}"* ]] && [[ "${PULL_REQUEST}" == "false" ]]
    then
        return 0
    else
        return 1
    fi
}
