#!/usr/bin/env bash
#
# Uses https://github.com/bucharest-gold/license-reporter/ to create license metadata with
# associated license text files.
#
set -e

function join { local IFS="$1"; shift; echo "${*}"; }

NODE_ROOT=$1
TARGET_DIR=$2
DIST=$3
NODE_EXE=${NODE_ROOT}/node/node
LICENSE_REPORTER_EXE=${NODE_ROOT}/bin/license-reporter
LICENSE_REPORTER_OPTS=${LICENSE_REPORTER_OPTS:---silent}
shift
shift
shift

if [ ! -d "${NODE_ROOT}" ]; then
   >&2 echo "Node install root directory ${NODE_ROOT} not found."
   exit 1
fi

if [ ! -x "${NODE_EXE}" ]; then
   >&2 echo "Node interpreter ${NODE_EXE} not found or not executable."
   exit 1
fi

if [ ! -f "${LICENSE_REPORTER_EXE}" ]; then
   >&2 echo "license-reporter ${LICENSE_REPORTER_EXE} not found.  Check that the npm module is installed."
   exit 1
fi

mkdir -p ${TARGET_DIR}

declare -a license_list
while [[ $# -gt 0 ]]
do
    TREE=$1
    shift

    pushd ${TREE}
    # Generates license metadata
    ${NODE_EXE} ${LICENSE_REPORTER_EXE} save --xml licenses.xml --full-dependency-tree ${LICENSE_REPORTER_OPTS}
    # Collects the license files from the node modules themselves
    ${NODE_EXE} ${LICENSE_REPORTER_EXE} report --full-dependency-tree ${LICENSE_REPORTER_OPTS}
    output=${TREE}/licenses/licenses.xml
    license_list+=("${output}")

    if compgen -G "licenses/*.TXT" > /dev/null; then
        cp -f licenses/*.TXT ${TARGET_DIR}
    fi
    popd
done

list=$(join "," ${license_list[@]})

pushd ${TARGET_DIR}
# Merges the license metadata together
${NODE_EXE} ${LICENSE_REPORTER_EXE} merge --merge-project-name ${DIST}_all  --merge-license-xmls ${list} --merge-output licenses.xml --outputDir . ${LICENSE_REPORTER_OPTS}
popd

