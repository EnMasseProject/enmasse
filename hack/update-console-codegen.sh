#!/usr/bin/env bash

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

set -o errexit
set -o nounset
set -o pipefail

# TODO: incorporate into update-codegen.sh

SCRIPTPATH="$(cd "$(dirname "$0")" && pwd -P)"

if which ragel >/dev/null 2>&1
then
    echo Generating Console filter lexer
    (cd $SCRIPTPATH/..; ragel -Z -G0 -o pkg/consolegraphql/filter/lex.go pkg/consolegraphql/filter/lex.rl)
fi

if which goyacc >/dev/null 2>&1
then
    echo Generating Console filter parser
    (cd $SCRIPTPATH/..; go generate pkg/consolegraphql/filter/support.go)
fi

echo Generating Console resource watchers
(cd $SCRIPTPATH/..; go generate pkg/consolegraphql/watchers/resource_watcher.go)


echo Generating Console GraphQL
(cd $SCRIPTPATH/..; GO111MODULE=on go run -mod=vendor $SCRIPTPATH/gqlgen.go -c console/console-server/src/main/resources/gqlgen.yml)

