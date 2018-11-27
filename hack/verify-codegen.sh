#!/usr/bin/env bash

#
# Copyright 2018, EnMasse authors.
# License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
#

set -o errexit
set -o nounset
set -o pipefail

SCRIPTPATH="$(cd "$(dirname "$0")" && pwd -P)"

TMPBASE="$(mktemp -d)"
TMPPROJ="$TMPBASE/github.com/enmasseproject/enmasse"

cleanup() {
    echo "Cleaning up: $TMPBASE"
    rm -rf "$TMPBASE"
}

echo "Using tmp base: $TMPBASE"

trap "cleanup" EXIT SIGINT

mkdir -p "$TMPPROJ"
cp -a "$SCRIPTPATH/../" "$TMPPROJ"

"$SCRIPTPATH/update-codegen.sh" --output-base "$TMPBASE"

echo "Comparing existing generated code with temporarily generated code"
if diff -Nur "$SCRIPTPATH/.." "$TMPPROJ"; then
    echo "No changes detected in generated code"
else
    echo "Generated code is out of date. Run hack/update-codegen.sh and commit the changes."
fi
