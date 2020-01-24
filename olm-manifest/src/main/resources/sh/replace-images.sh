#!/bin/bash -ex
# Script that replaces image version in the operator CSV to work with Operator Hub tooling
#
# The list of components to derive are automatically added as part of the build process for the script.
#
# The script itself is invoked during the build of the container image where it will allow overriding the image urls via ${MY_IMAGE_PULL_URL}

CSV_FILE=/manifests/${olm.version}/${application.bundle.prefix}.${olm.version}.clusterserviceversion.yaml
export ${env.IMAGE_ENV}
COMPONENTS=$(echo ${env.IMAGE_ENV} | sed -e 's/ /\n/g' | cut -f 1 -d '=')

function replace_images() {
    local -a component normalized key replacement replacement_key

    for component in ${COMPONENTS}; do
        echo "Inspecting component ${component}"
        if [[ "${component}" == *_IMAGE ]]; then
            replacement_key=${component}_PULL_URL

            replacement=$(printenv ${replacement_key}) || found=0

            if [[ -v "${replacement_key}" ]]; then
                sed -e "s,\${${component}},${replacement},g" -i ${CSV_FILE}
            else
                value=$(printenv ${component})
                sed -e "s,\${${component}},${value},g" -i ${CSV_FILE}
                missing_vars+=("${replacement_key}")
            fi
        fi
    done

    [[ "${#missing_vars[@]}" -eq 0 ]] || {
        echo "External variables not set (defaults used): ${missing_vars[*]}" >&2
    }
}

main() {
       replace_images
}

[[ "${BASH_SOURCE[0]}" == "$0" ]] && {
    main
}
