#!/usr/bin/env bash

OLD_SPACE_SIZE_PERCENT=${OLD_SPACE_SIZE_PERCENT:-80}
CGROUP_FILE='/sys/fs/cgroup/memory/memory.limit_in_bytes'

# Ensure that we have a suitable node interpreter.  This ought to be declared declaratively in the package.json, within an engines section,
# but we currently don't install the package so this wouldn't be enforced.
REQUIRED_NODE_MAJOR=6
NODE_MAJOR=$(node -p 'process.version.match("^v?(\\d+)\.")[1]')
if [[ ${NODE_MAJOR} -lt ${REQUIRED_NODE_MAJOR} ]]; then
    2>&1 echo "Node major version ${NODE_MAJOR} [$(node --version)] too low - require ${REQUIRED_NODE_MAJOR}"
    exit 1
fi

if [[ ! -z "${_NODE_OPTIONS}" ]]; then
    set -- ${_NODE_OPTIONS} "${@}"
fi

# Set max_old_space_size w.r.t the container's memory, unless caller has supplied --max_old_space_size
for arg; do
  if [[ "${arg}" =~ ^--max_old_space_size.* ]]; then
    OLD_SPACE_SIZE_PERCENT=0
    break
  fi
done

if [[ -f "${CGROUP_FILE}" && "${OLD_SPACE_SIZE_PERCENT}" -gt 0 ]]; then
    CONTAINTER_BYTES=$(cat ${CGROUP_FILE})
    MAX_OLD_SPACE_SIZE=$(( ${CONTAINTER_BYTES} / 100 * ${OLD_SPACE_SIZE_PERCENT} ))
    MAX_OLD_SPACE_SIZE_MI=$(( ${MAX_OLD_SPACE_SIZE} / ( 1024 * 1024 ) ))
    set -- "--max_old_space_size=${MAX_OLD_SPACE_SIZE_MI}" "${@}"
fi

exec node "${@}"
