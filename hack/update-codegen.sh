#!/usr/bin/env bash

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

#TODO wire in console code generation

set -o errexit
set -o nounset
set -o pipefail

SCRIPTPATH="$(cd "$(dirname "$0")" && pwd -P)"

"$SCRIPTPATH/generate-groups.sh" "all" \
    github.com/enmasseproject/enmasse/pkg/client \
    github.com/enmasseproject/enmasse/pkg/apis \
    "admin:v1beta1 admin:v1beta2 enmasse:v1beta1 user:v1beta1 iot:v1alpha1" \
    --go-header-file "${SCRIPTPATH}/header.txt" \
    "$@"
