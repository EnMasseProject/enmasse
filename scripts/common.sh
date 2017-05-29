#!/bin/bash
function runcmd() {
    local cmd=$1
    local description=$2

    if [ -z $GUIDE ]; then
        eval $cmd
    else
        echo "$description:"
        echo ""
        echo "    $cmd"
        echo ""
    fi
}

function docmd() {
    local cmd=$1
    if [ -z $GUIDE ]; then
        $cmd
    fi
}
