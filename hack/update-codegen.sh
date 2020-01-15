#!/usr/bin/env bash

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

set -o errexit
set -o nounset
set -o pipefail

SCRIPTPATH="$(cd "$(dirname "$0")" && pwd -P)"

if which -s ragel
then
    echo Generating Console filter lexer
    (cd $SCRIPTPATH/..; ragel -Z -G0 -o pkg/consolegraphql/filter/lex.go pkg/consolegraphql/filter/lex.rl)
fi

if which -s goyacc
then
    echo Generating Console filter parser
    (cd $SCRIPTPATH/..; go generate pkg/consolegraphql/filter/support.go)
fi

echo Generating Console resource watchers
(cd $SCRIPTPATH/..; go generate pkg/consolegraphql/watchers/resource_watcher.go)


# KWTODO fix on Travis - not finding the vendor code??
#echo Generating Console GraphQL
#(go run $SCRIPTPATH/gqlgen.go -c console/console-server/src/main/resources/gqlgen.yml)


"$SCRIPTPATH/generate-groups.sh" "all" \
    github.com/enmasseproject/enmasse/pkg/client \
    github.com/enmasseproject/enmasse/pkg/apis \
    "admin:v1beta1 admin:v1beta2 enmasse:v1beta1 user:v1beta1 iot:v1alpha1" \
    --go-header-file "${SCRIPTPATH}/header.txt" \
    "$@"

