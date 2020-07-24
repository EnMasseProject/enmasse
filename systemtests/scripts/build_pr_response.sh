#!/usr/bin/env bash

RESULTS_PATH=${1}
TEST_CASE=${2}
TEST_PROFILE=${3}
BUILD_ID=${4:-0}
OCP_VERSION=${5:-3}
JOB_STATUS=${6}

JSON_FILE_RESULTS=results.json

function get_test_count () {
    _TEST_TYPE=${1}
    _VALUES=$(find "${RESULTS_PATH}" -name "TEST*.xml" -type f -print0 | xargs -0 sed -n "s#.*${_TEST_TYPE}=\"\([0-9]*\)\".*#\1#p")
    _TEST_COUNTS_ARR=$(echo "${_VALUES}" | tr " " "\n")
    _TEST_COUNT=0

    for item in ${_TEST_COUNTS_ARR}
    do
        _TEST_COUNT=$((_TEST_COUNT + item))
    done

    echo ${_TEST_COUNT}
}

TEST_COUNT=$(get_test_count "tests")
TEST_ERRORS_COUNT=$(get_test_count "errors")
TEST_SKIPPED_COUNT=$(get_test_count "skipped")
TEST_FAILURES_COUNT=$(get_test_count "failures")

TEST_ALL_FAILED_COUNT=$((TEST_ERRORS_COUNT + TEST_FAILURES_COUNT))


if [[ "${OCP_VERSION}" == "4" ]]; then
  BUILD_ENV="crc"
else
  BUILD_ENV="oc cluster up"
fi

SUMMARY="**TEST_PROFILE**: ${TEST_PROFILE}\n**TEST_CASE:** ${TEST_CASE}\n**TOTAL:** ${TEST_COUNT}\n**PASS:** $((TEST_COUNT - TEST_ALL_FAILED_COUNT - TEST_SKIPPED_COUNT))\n**FAIL:** ${TEST_ALL_FAILED_COUNT}\n**SKIP:** ${TEST_SKIPPED_COUNT}\n**BUILD_NUMBER:** ${BUILD_ID}\n**BUILD_ENV:** ${BUILD_ENV}\n"

FAILED_TESTS=$(find "${RESULTS_PATH}" -name 'TEST*.xml' -type f -print0 | xargs -0 awk '/<testcase.*>/{ getline x; if (x ~ "<error" || x ~ "<failure") {  gsub(/classname=|name=|\"/, "", $0); if ($3 ~ "time=") {print "\\n- " $2 } else {print "\\n- " $2 " in " $3 }}}')
echo "${FAILED_TESTS}"
echo "Creating body ..."


TITLE="Test Summary"
BODY=""
MARK=":question:" # init with unknown default

if [[ "${TEST_COUNT}" == 0 ]]
then
  # no tests executed
  MARK=":heavy_exclamation_mark:"
  FAILED_TEST_BODY="### :heavy_exclamation_mark: No tests executed :heavy_exclamation_mark:"
elif [[ "${TEST_ALL_FAILED_COUNT}" == 0 ]]
then
  # no failed tests, may be overridden by the following job status check
  MARK=":heavy_check_mark:"
else
  # some failed
  MARK=":x:"
  FAILED_TEST_BODY="### :heavy_exclamation_mark: Test Failures :heavy_exclamation_mark:${FAILED_TESTS}"
fi

# finally override status
case "${JOB_STATUS}" in
  ABORTED)
    MARK=":white_circle:"
    TITLE="**Build Aborted**"
    ;;
  FAILURE)
    MARK=":heavy_exclamation_mark:"
    TITLE="**Build Failed**"
    ;;
esac

BODY="### ${MARK} ${TITLE} ${MARK}\n${SUMMARY}${FAILED_TEST_BODY}"

# encode as JSON in the field 'body'
echo "${BODY}" | jq -sR '{"body": .}' | tee "${JSON_FILE_RESULTS}"
