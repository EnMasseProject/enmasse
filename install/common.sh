#!/bin/bash
function runcmd() {
    local cmd=$1
    local description=$2

    if [ "$GUIDE" == "true" ]; then
        echo "$description:"
        echo ""
        echo "    $cmd"
        echo ""
    else
        bash -c "$cmd"
    fi
}

function docmd() {
    local cmd=$1
    if [ -z $GUIDE ] || [ "$GUIDE" == "false" ]; then
        $cmd
    fi
}

function tempdir() {
    echo `mktemp -d /tmp/enmasse-deploy.XXXXXX`
}
