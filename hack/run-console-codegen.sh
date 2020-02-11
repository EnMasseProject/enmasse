#!/usr/bin/env bash

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

# Updates the generated code associated with the EnMasse Console
# This script is separate from run-codegen.sh as the gqlgemn stage does not support generating output to an
# alternative location.

ROOT="$(cd "$(dirname "$0")"/.. && pwd -P)"

cd "${ROOT}"

if which ragel >/dev/null 2>&1
then
    echo Generating Console filter lexer
    ragel -Z -G0 -o pkg/consolegraphql/filter/lex.go pkg/consolegraphql/filter/lex.rl
else
    echo Skipped the generation Console filter lexer
fi

if which goyacc >/dev/null 2>&1
then
    echo Generating Console filter parser
    goyacc -o pkg/consolegraphql/filter/parser.go -p Filter pkg/consolegraphql/filter/parser.y
else
    echo Skipped the generation Console filter parser
fi

echo Generating Console resource watchers
go generate pkg/consolegraphql/watchers/resource_watcher.go

echo Generating Console GraphQL
GO111MODULE=on go run -mod=vendor hack/gqlgen.go -c console/console-server/src/main/resources/gqlgen.yml

